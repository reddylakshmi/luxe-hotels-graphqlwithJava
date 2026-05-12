# Production Deploy Runbook

Operational guide for taking the Luxe federated GraphQL platform from
the local-dev shape (see [`README.md`](../README.md)) to a production
deployment sized for **30+ brands, 9000+ hotels, 100+ countries** —
i.e. Marriott-class scale.

This document is the contract between the code shipped in this repo
and the production environment that runs it. It covers infra
prerequisites, the cache strategy already wired into the code, the
licenses and managed services you need to procure, and the
invalidation discipline that keeps the caches honest.

---

## 1. Target architecture

```
                       ┌─────────────────────────────┐
   global users ──►    │  CDN (Cloudflare / Fastly)  │   public catalog HTML
                       │  s-maxage + stale-while-rev │   ~80% hit ratio target
                       └──────────────┬──────────────┘
                                      ▼
                       ┌─────────────────────────────┐
                       │  Web tier (Next.js 14)      │   ISR + tag-based revalidate
                       │  multi-region, autoscaled   │   /brands, /hotels/*, /search
                       └──────────────┬──────────────┘
                                      ▼ POST /graphql
                       ┌─────────────────────────────┐
                       │  Apollo Router 2.x          │   APQ (in-memory)
                       │  3+ replicas / region       │   query-plan cache (4 k)
                       │  Enterprise license         │   entity cache → Redis
                       └──────────────┬──────────────┘
                                      ▼ /graphql per subgraph
   ┌──────────────────────────────────┼──────────────────────────────────┐
   ▼                                  ▼                                  ▼
 ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
 │ property │   │ pricing  │   │ content  │   │ loyalty  │   │   …7 more
 │ Caffeine │   │ Caffeine │   │ Caffeine │   │ Caffeine │   │
 └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘
      │              │              │              │              │
      ▼              ▼              ▼              ▼              ▼
 ┌──────────────────────────────────────────────────────────────────────┐
 │ Redis Cluster (managed: ElastiCache / Memorystore / Azure Cache)     │
 │ • rate-limit buckets (Bucket4j-Lettuce)                              │
 │ • Apollo entity cache namespace: luxe:ec                             │
 │ • query-plan cache namespace:    luxe:qp                             │
 │ • APQ namespace:                 luxe:apq  (when Redis-backed)       │
 └──────────────────────────────────────────────────────────────────────┘
      ┌──────────────────────────────────────────────────────────────┐
      │ PostgreSQL primary + read replicas │ DynamoDB / Cosmos DB    │
      │ (reservations, availability)       │ (catalog: hotels, brands)│
      ├──────────────────────────────────────────────────────────────┤
      │ OpenSearch (search + autocomplete) │ Kafka (invalidation)    │
      └──────────────────────────────────────────────────────────────┘
```

---

## 2. What's already wired into the code

| Layer | Where | Default | Activation |
|---|---|---|---|
| In-process catalog cache (Caffeine) | `common/.../config/CachingConfig.java`, `@Cacheable` on `PropertyDataFetcher` + `PricingDataFetcher` | always on | none — ships active |
| In-memory rate-limit buckets | `common/.../security/InMemoryRateLimitStore.java` | always on | none — default backend |
| Redis-backed rate-limit buckets | `common/.../security/RedisRateLimitStore.java` | dormant | set `luxe.security.rate-limit.backend=redis` |
| Apollo APQ (in-memory) | `router/router.yaml` | on | none |
| Apollo query-plan cache | `router/router.yaml` `query_planning.cache.in_memory.limit: 4096` | on | none |
| Apollo subgraph dedup | `router/router.yaml` `traffic_shaping.all.deduplicate_query: true` | on | none |
| Apollo entity cache | `router/router.yaml` `preview_entity_cache` block (commented) | off | uncomment + license + Redis |
| Web ISR with cache tags | `web/src/lib/graphql.ts` + `web/src/app/brands/*` | on | none — ships active |

The lines that flip dormant features on are all property-driven
(`luxe.security.rate-limit.backend`, the commented `preview_entity_cache`
block) — there is no code change required to activate them.

---

## 3. Required managed services

| Service | Purpose | Sizing baseline | Notes |
|---|---|---|---|
| **Redis Cluster** (ElastiCache / Memorystore / Azure Cache) | rate-limit buckets, entity cache, query-plan cache | 3 shards × 2 replicas, `cache.r7g.large` or equivalent (~13 GB RAM) | Enable AUTH; in-VPC only; cross-AZ replication |
| **Apollo GraphOS** | router Enterprise license + schema registry + ops metrics | one graph per environment | Set `APOLLO_KEY` + `APOLLO_GRAPH_REF` on the router; an offline `--license` file is the air-gapped alternative |
| **PostgreSQL** | reservations, availability, payment refs | RDS / Cloud SQL, multi-AZ, 1 primary + 2 read replicas per region | Read replicas behind a `spring.datasource.read.*` config (not yet wired in this repo) |
| **DynamoDB / Cosmos DB** | catalog (hotels, brands, room types, media) | partition by `brandId`; global tables enabled | When the property subgraph swaps off the mock, the Caffeine layer absorbs the read traffic |
| **OpenSearch / Elastic** | hotel + destination autocomplete | 3-node cluster, 200 GB | Replaces the in-memory `destinationSuggestions` resolver |
| **CDN** (Cloudflare / Fastly / CloudFront) | edge cache for anonymous catalog pages | per-region POPs, `s-maxage=300, stale-while-revalidate=3600` | See [§ 6 CDN](#6-cdn-headers) |
| **Kafka / Kinesis** | invalidation events (`brand.updated`, `inventory.changed`) | 3-broker, 7-day retention | Web service consumes → `revalidateTag()`; gateway / subgraphs consume → `@CacheEvict` |
| **Observability** | OpenTelemetry collector → APM (Datadog / New Relic / Honeycomb) | per-region collectors | Already exporting actuator metrics; OTel SDK addition is a small follow-up |

---

## 4. Step-by-step rollout

### 4.1 Phase A — single region, free tier (matches local-dev shape)

Already deployable as-is. Caffeine + in-memory rate limits + APQ are
on by default; no Redis or Apollo Enterprise license required.

```bash
mvn -q clean install -DskipTests
bash scripts/start-subgraphs.sh          # 10 subgraphs come up
./router/router --config router/router.yaml --supergraph supergraph.graphqls
```

Web (`luxe-hotels-web`) deploys to any Node host. Sanity check the
cache headers in production:

```bash
curl -sI https://<your-host>/brands | grep -i cache-control
curl -sI https://<your-host>/brands/brand-lux-001 | grep -i cache-control
```

ISR-driven pages return `cache-control: s-maxage=300, stale-while-revalidate=...`
when deployed on Vercel / a CDN-aware host.

### 4.2 Phase B — horizontal scale (Redis rate limiting)

Required as soon as you run more than one replica of any subgraph,
otherwise the in-memory buckets fragment and the effective rate
limit multiplies by N.

```bash
# Provision Redis (example: AWS ElastiCache cluster mode disabled)
# Resulting endpoint: clustercfg.luxe-prod-001.usw2.cache.amazonaws.com:6379

# Per-subgraph env:
export LUXE_SECURITY_RATE_LIMIT_BACKEND=redis
export SPRING_DATA_REDIS_HOST=clustercfg.luxe-prod-001.usw2.cache.amazonaws.com
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD=<from-secrets-manager>
```

Spring Boot picks up `spring.data.redis.*` directly. The
`RedisRateLimitStore` connects at startup and logs the configured
capacity. Existing in-memory buckets are dropped — buckets warm
from empty over the first refill window.

Validate:

```bash
# From inside the VPC:
redis-cli -h <endpoint> KEYS 'rl:*' | head
# Should populate as traffic flows; values are CAS-encoded byte blobs.
```

For Redis Cluster: swap `RedisClient` → `RedisClusterClient` in
[`RedisRateLimitStore.java`](../common/src/main/java/com/luxe/common/security/RedisRateLimitStore.java)
and pass cluster-mode URLs. The Bucket4j-Lettuce builder has a
cluster-aware variant; one-line change.

### 4.3 Phase C — gateway entity cache (Enterprise)

Once `APOLLO_KEY` + `APOLLO_GRAPH_REF` are provisioned in the
router's environment (or `--license` is supplied), uncomment the
`preview_entity_cache` block in `router/router.yaml` and restart.
Per-subgraph TTLs are already aligned to data churn:

| Subgraph | TTL | Why |
|---|---|---|
| `property` | 5 min | brand / hotel / room-type catalog drifts slowly |
| `content` | 5 min | CMS articles, brand stories |
| `pricing` | 30 s | availability changes fast; per-resolver `Cache-Control` overrides this |
| `experiences` | 1 min | restaurants / spa metadata |
| `reservations` | disabled | per-user write-side data |
| `loyalty` | disabled | points balance must be live during redemption |
| `guest` | disabled | PII; can't share across users |
| `meetings` | disabled | per-user RFP state |
| `notifications` | disabled | per-user inbox |
| `corporate` | disabled | per-account scoping |

Subgraphs can emit standard `Cache-Control` HTTP headers on individual
resolvers to override the static TTL — see [§ 7 invalidation
contract](#7-invalidation-contract).

### 4.4 Phase D — multi-region

- Deploy router + subgraphs to each region; each region gets its own
  Redis Cluster (low cross-region replication cost since catalog data
  is the same everywhere).
- DynamoDB Global Tables / Cosmos DB multi-region writes for the
  catalog store.
- PostgreSQL: primary in one region with cross-region read replicas;
  reservation writes always hit the primary.
- DNS-level latency-based routing (Route 53 / Cloud DNS) splits
  traffic to nearest region.
- Kafka: MirrorMaker / cross-cluster replication for the invalidation
  topic so a brand edit in `us-west-2` invalidates caches in
  `eu-west-1` within seconds.

---

## 5. Environment variables (per service)

### Router

```bash
APOLLO_ROUTER_LOG=info
APOLLO_ROUTER_CONFIG_PATH=/etc/router/router.yaml
APOLLO_ROUTER_SUPERGRAPH_PATH=/etc/router/supergraph.graphqls
APOLLO_KEY=service:luxe-prod:xxx                # GraphOS service token
APOLLO_GRAPH_REF=luxe@prod                       # graph variant
APOLLO_TELEMETRY_DISABLED=false                  # send anonymized usage; flip for air-gap
LUXE_INTROSPECTION=false                         # production
LUXE_SANDBOX_ENABLED=false                       # production
```

### Subgraphs (all 10)

```bash
SERVER_PORT=4001                                 # per subgraph
SPRING_PROFILES_ACTIVE=production
LUXE_SECURITY_RATE_LIMIT_BACKEND=redis
LUXE_SECURITY_RATE_LIMIT_CAPACITY=120            # default 120 req / 60 s
LUXE_SECURITY_RATE_LIMIT_WINDOW_SECONDS=60
SPRING_DATA_REDIS_HOST=...                       # managed Redis endpoint
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<from-secrets>
SPRING_DATA_REDIS_SSL_ENABLED=true               # when crossing AZ boundaries
LUXE_JWT_SECRET=<from-secrets>                   # shared with the router if gateway-validated
JAVA_OPTS="-XX:+UseG1GC -Xms1g -Xmx2g"
```

### Web (Next.js)

```bash
NEXT_PUBLIC_GRAPHQL_URL=https://router.luxehotels.com/
NEXT_RUNTIME=nodejs
NODE_ENV=production
LUXE_JWT_SECRET=<from-secrets>                   # signs the session cookie
```

---

## 6. CDN headers

The web layer already returns the right cache headers on its public
catalog pages — see `revalidate = 300` on
[`web/src/app/brands/page.tsx`](../../luxe-hotels-web/src/app/brands/page.tsx)
and `/brands/[id]/page.tsx`. The CDN policy that consumes them:

| Path | CDN policy | Notes |
|---|---|---|
| `/` | `s-maxage=300, stale-while-revalidate=3600` | hero + featured hotels; anonymous-only |
| `/brands` | `s-maxage=300, stale-while-revalidate=3600` | brand-list view |
| `/brands/[id]` | `s-maxage=300, stale-while-revalidate=3600` | brand-detail catalog |
| `/hotels/[id]` (no query) | `s-maxage=300, stale-while-revalidate=3600` | hotel-detail catalog tab |
| `/hotels/[id]?*` (search context) | `private, no-store` | personalised by stay / picker |
| `/hotels/[id]/rates*` | `private, no-store` | live pricing |
| `/hotels/[id]/book*`, `/account/*`, `/trips/*` | `private, no-store` | authed |
| `/api/*` | `private, no-store` | server actions |

Vary headers must include `Accept-Encoding` and `Authorization` so
authed responses don't leak into the anonymous cache.

---

## 7. Invalidation contract

Cache invalidation is the cost of caching. Every mutation that
changes data behind a cached entity must invalidate that entity, or
visitors will see stale content until the TTL expires.

| Mutation | Web tag(s) to revalidate | Subgraph eviction | Router entity cache eviction |
|---|---|---|---|
| `submitReview`, `markReviewHelpful` | `catalog:hotel:<id>` (when reviews surface in hotel detail) | `@CacheEvict(value="catalog.featuredHotels", allEntries=true)` if review affects ranking | Apollo entity cache `Hotel:<id>` via the invalidation endpoint |
| (future) `updateBrand` | `catalog:brand:<id>`, `catalog:brands` | `@CacheEvict(value="catalog.brand", key="'id:'+#id")`, `@CacheEvict(value="catalog.brandsList", allEntries=true)` | `Brand:<id>` |
| (future) `publishRatePlan` | `catalog:hotel:<id>` (lowest-rate displays) | `@CacheEvict` not needed (pricing isn't Caffeined) | `Hotel:<id>` (rate fields) |
| `createReservation` | none — booking is per-user | none | none |

Web side: server actions call `revalidateTag('catalog:brand:<id>')`
after a write. Tags are declared at the read site —
[`web/src/app/brands/[id]/page.tsx`](../../luxe-hotels-web/src/app/brands/%5Bid%5D/page.tsx)
already passes `tags: ['catalog:brand:<id>']` to `gqlFetch`.

Router entity cache: invalidation goes through the Enterprise
invalidation endpoint (POST to `/cache-invalidation/...` — configure
in `preview_entity_cache.invalidation`). Kafka consumers on the
router pod POST to this endpoint when they see a `brand.updated`
event.

Subgraph Caffeine: `@CacheEvict` on the mutation resolver, OR a
Kafka listener that calls `cacheManager.getCache(name).evict(key)`.
The eviction is local to one JVM; in a multi-replica deployment
either:
- evict on every replica (consume the Kafka topic in each pod), OR
- shrink Caffeine TTL to a number you can tolerate as worst-case
  stale (5 min today).

---

## 8. SLOs and runbook signals

| Signal | Target | Source | Alert |
|---|---|---|---|
| Router p99 latency | < 250 ms | OpenTelemetry trace | warn > 400 ms, page > 1 s |
| Web LCP (catalog pages) | < 1.5 s | RUM (real-user metrics) | warn > 2 s |
| Caffeine hit ratio (per cache) | > 90% on `catalog.brand`, `pricing.specialRates`; > 70% on `catalog.featuredHotels` | actuator `/actuator/caches` (when enabled) + Micrometer | warn < 60% — likely cache thrash or wrong key shape |
| Redis CPU | < 50% | CloudWatch / Cloud Monitoring | warn > 70%; page > 85% |
| Rate-limit 429 rate | < 0.5% of requests | router metrics | warn > 1% — legit traffic likely tripping limits |
| Apollo entity cache hit ratio | > 80% on `property`, `content` | Apollo GraphOS dashboard | warn < 50% |
| Subgraph 5xx | < 0.1% | per-subgraph actuator | page > 1% |
| Reservation success rate | > 99.5% | reservations subgraph | page < 99% (likely DB / inventory issue) |
| Cache stampede on mutation | 0 events / min | Kafka consumer lag on invalidation topic | warn > 30 s lag |

`/actuator/caches`, `/actuator/health`, `/actuator/metrics`,
`/actuator/prometheus` are already exposed by every subgraph; ship
them through the OTel collector to your APM.

---

## 9. Pre-flight checklist

Before a green-light deploy:

- [ ] Apollo GraphOS variant `luxe@prod` has the latest supergraph schema
- [ ] `APOLLO_KEY` + `APOLLO_GRAPH_REF` injected from secrets manager (not hard-coded)
- [ ] Redis Cluster reachable from every subgraph pod; AUTH + TLS enabled
- [ ] PostgreSQL read replicas in each region; failover tested
- [ ] DynamoDB global tables enabled for catalog; replication lag < 1 s
- [ ] CDN cache rules deployed (per [§ 6](#6-cdn-headers)); purge runbook in place
- [ ] Kafka topic `luxe.invalidation` provisioned with 7-day retention
- [ ] OpenTelemetry collector running per region; dashboards loaded
- [ ] On-call rotation set; runbooks linked from alerts
- [ ] Load test against staging at 3× expected peak; rate-limit + cache hit ratios validated
- [ ] Rollback plan documented (router schema rollback, subgraph image rollback)
- [ ] DR drill executed in the last 90 days

---

## 10. References

- Apollo Router config: [`router/router.yaml`](../router/router.yaml)
- Cache config (Caffeine): [`common/.../config/CachingConfig.java`](../common/src/main/java/com/luxe/common/config/CachingConfig.java)
- Rate-limit stores: [`common/.../security/`](../common/src/main/java/com/luxe/common/security/)
- Local Redis (dev): [`docker-compose.yml`](../docker-compose.yml) service `redis`
- Web cache wrapper: [`luxe-hotels-web/src/lib/graphql.ts`](../../luxe-hotels-web/src/lib/graphql.ts)
- Apollo Router Enterprise features: <https://www.apollographql.com/docs/router/configuration/overview/#features>
- Apollo Router entity cache: <https://www.apollographql.com/docs/router/configuration/entity-caching/>
- Bucket4j-Lettuce: <https://bucket4j.com/9.0/toc.html#bucket4j-redis>
