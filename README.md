# Luxe Hotels — Federated GraphQL Platform (Java)

A reference Apollo Federation 2 implementation for a luxury-hotel platform, built as
ten independent Java + Spring Boot subgraph services composed behind an Apollo Router.
Backed by in-memory mock data — no database, no external dependencies.

The point of this codebase is to show how a real, federated, multi-domain GraphQL API
hangs together end-to-end: schema decomposition, entity references and stitching,
shared types with `@shareable`, custom scalars, JWT authentication that doesn't break
federation introspection, and the operational glue (composition, routing, health
checks) that makes the whole thing serve queries.

---

## Architecture

```
                      ┌─────────────────────────────┐
                      │   Apollo Router (port 4000) │
                      │   /                         │
                      │   sandbox + introspection   │
                      └──────────────┬──────────────┘
                                     │
       ┌─────────────┬───────────────┼────────────────┬──────────────┐
       ▼             ▼               ▼                ▼              ▼
 property:4001   guest:4002    pricing:4003   reservations:4004  loyalty:4005
       ▼             ▼               ▼                ▼              ▼
 content:4006  experiences:4007  meetings:4008  notifications:4009  corporate:4010
```

Each subgraph is an independent Spring Boot 3.3 / Java 21 service using the Netflix DGS
framework (`graphql-dgs-spring-graphql-starter` 8.7.1). The Apollo Router (v2.14)
composes their schemas into a federated supergraph and routes operations to the
correct subgraphs at query time.

| Subgraph        | Port | Owns                                                                                |
|-----------------|------|-------------------------------------------------------------------------------------|
| `property`      | 4001 | Hotels, brands, room types, amenities, restaurants, spas, reviews, media           |
| `guest`         | 4002 | Guest profiles, addresses, preferences, payment methods, travel companions          |
| `pricing`       | 4003 | Rate plans, promotions, gift cards, availability, demand signals                    |
| `reservations`  | 4004 | Reservations, dining/spa/transport bookings, check-in, folio, digital keys          |
| `loyalty`       | 4005 | Loyalty accounts (MEMBER → AMBASSADOR), tier progress, points, certificates, partners, challenges |
| `content`       | 4006 | Articles, travel inspirations, brand story, deal spotlights (all multilingual)      |
| `experiences`   | 4007 | Experiences, spa treatments, restaurant + golf availability, slot-token bookings    |
| `meetings`      | 4008 | Event spaces, RFP lifecycle, group blocks, catering menus                           |
| `notifications` | 4009 | Notifications, channel/category preferences, push devices, message threads          |
| `corporate`     | 4010 | Corporate accounts, travel policies (rate caps, approval chains), travel reports    |

Cross-subgraph references resolve through Apollo Federation's `_entities` mechanism:
the property service owns `Hotel`, the guest service owns `GuestProfile`, and other
subgraphs extend those types with their own fields (e.g. `Hotel.eventSpaces` from
meetings, `GuestProfile.loyaltyAccount` from loyalty).

---

## Prerequisites

| Tool                  | Version    | Why |
|-----------------------|------------|-----|
| Java                  | 21+        | Records, sealed types, virtual threads |
| Maven                 | 3.9+       | Build (project uses IntelliJ-bundled mvn by default) |
| `rover` CLI           | 0.38+      | Composes the federated supergraph schema |
| Apollo Router         | 2.14+      | Stitches the subgraph schemas into one federated endpoint |

Install `rover` and the Apollo Router (one-time, into your home dir / project dir):

```bash
curl -sSL https://rover.apollo.dev/nix/latest | sh                  # → ~/.rover/bin/rover
curl -sSL https://router.apollo.dev/download/nix/latest | sh        # → ./router/router
```

---

## Quick start

From the project root, in **one terminal**:

```bash
# 1. Build all 11 modules into runnable jars (~2-3s clean build)
mvn clean package -DskipTests

# 2. Boot all 10 subgraph services in the background, wait for ready (~12s)
./scripts/start-subgraphs.sh

# 3. Compose the federated supergraph schema
APOLLO_ELV2_LICENSE=accept ~/.rover/bin/rover supergraph compose \
  --config supergraph.yaml --output supergraph.graphqls

# 4. Start the router (foreground; Ctrl+C to stop)
./router/router --config router/router.yaml --supergraph supergraph.graphqls
```

Or as a one-liner:

```bash
mvn clean package -DskipTests \
  && ./scripts/start-subgraphs.sh \
  && APOLLO_ELV2_LICENSE=accept ~/.rover/bin/rover supergraph compose --config supergraph.yaml --output supergraph.graphqls \
  && ./router/router --config router/router.yaml --supergraph supergraph.graphqls
```

Once the router logs `GraphQL endpoint exposed at http://0.0.0.0:4000/`:

- **GraphQL endpoint**: `http://localhost:4000/`
- **Apollo Sandbox UI**: `http://localhost:4000/` (open in browser)
- **Router health**: `http://localhost:8088/health`
- **Per-subgraph GraphiQL**: `http://localhost:400N/graphql` for direct subgraph testing

---

## Stopping everything

```bash
xargs kill < /tmp/luxe-run/pids       # stops all 10 subgraphs
# Ctrl+C in the router's terminal stops the router
```

The start script tracks subgraph PIDs in `/tmp/luxe-run/pids` and writes per-subgraph
logs to `/tmp/luxe-run/<name>.log`.

---

## Example federated queries

### A query that crosses three subgraphs in one request

```graphql
{
  featuredHotels(first: 1) {
    id
    name
    starRating
    location {
      address { city countryCode }
      coordinates { latitude longitude }
    }

    # resolved by experiences subgraph via federated Hotel reference
    experiences(category: SPA_WELLNESS) {
      name
      durationMinutes
      pricePerPerson { amount currency }
    }

    # resolved by meetings subgraph
    eventSpaces(filter: { minCapacity: 100 }) {
      name
      capacityStyles { setup capacity }
      rateCard { fullDay { amount currency } }
    }
  }
}
```

### Destination autocomplete (typeahead)

Lightweight ranked list of cities, countries, and hotels matching a partial
query — backs the search bar's typeahead. Returns prefix matches first, then
substring matches, broad-first within each tier.

```graphql
{
  destinationSuggestions(query: "Hyd", limit: 6) {
    type            # CITY | COUNTRY | HOTEL
    label
    sublabel        # "India · 3 hotels", "Plot 17, Cyber Towers Road, Hyderabad, India", etc.
    hotelId         # set when type == HOTEL
    hotelSlug
    city
    country
    countryCode
  }
}
```

### A multilingual content query with locale fallback

```graphql
{
  featuredArticles(first: 2, locale: "fr") {
    title { text locale fallbackUsed }
    category
    readTimeMinutes
  }
}
```

### Loyalty points valuation (uses `CurrencyCode` custom scalar)

```graphql
{
  pointsValuation(points: 75000, currency: "EUR") {
    cashValue { amount currency }
    bestUse
    comparisonRedemptions { name points approxValue { amount } }
  }
}
```

### Guest-context queries (require auth)

Reservations, loyalty account details, message threads, and corporate accounts all
require an `Authorization: Bearer <jwt>` header. The router propagates the header to
the relevant subgraph(s) per `router.yaml`.

---

## Project structure

```
.
├── common/                           # Shared code: scalars, errors, auth, pagination
│   └── src/main/java/com/luxe/common/
│       ├── auth/                     # JwtService, SubgraphAuthInterceptor, SecurityConfig, AuthContext
│       ├── config/                   # CommonGraphQLConfig (registers all custom scalars)
│       ├── error/                    # NotFoundError, ValidationError, etc.
│       ├── pagination/               # Connection, Edge, PageInfo helpers (Relay-style)
│       └── scalar/                   # Money record
│
├── subgraph-{name}/                  # x10
│   ├── pom.xml
│   ├── Dockerfile                    # multi-stage build (jdk → jre-alpine)
│   └── src/main/
│       ├── java/com/luxe/{name}/
│       │   ├── {Name}Application.java
│       │   ├── config/               # subgraph-specific Spring config (auth interceptor wiring)
│       │   ├── datasource/           # interface + in-memory mock impl
│       │   ├── resolver/             # @DgsComponent: queries, mutations, @DgsData, @DgsEntityFetcher
│       │   └── schema/types/         # POJOs/records for each GraphQL type
│       └── resources/
│           ├── application.yml       # port, JWT secret, schema-inspection disabled, local profile
│           └── schema/schema.graphqls
│
├── router/
│   ├── router                        # downloaded Apollo Router binary
│   ├── router.yaml                   # local-dev config (sandbox, CORS, override URLs)
│   └── router-docker.yaml            # Docker Compose variant
│
├── scripts/
│   └── start-subgraphs.sh            # boots all 10 jars in background, waits for ready
│
├── supergraph.yaml                   # rover compose config (lists 10 subgraphs)
├── supergraph.graphqls               # composed federated schema (output of rover)
├── docker-compose.yml                # alternative orchestration via Docker
├── pom.xml                           # parent POM with dependency management
└── README.md
```

---

## Configuration

### Per-subgraph application.yml

Each subgraph reads:

| Property                           | Default                          | Effect |
|------------------------------------|----------------------------------|--------|
| `server.port`                      | port number (env-overridable via `PORT`) | listen port |
| `spring.graphql.graphiql.enabled`  | `true`                           | enables GraphiQL UI at `/graphiql` |
| `spring.graphql.path`              | `/graphql`                       | GraphQL endpoint |
| `spring.graphql.schema.inspection.enabled` | `false`                  | disables Spring GraphQL's schema inspector (incompatible with our Map-based connection pattern) |
| `luxe.jwt.secret`                  | `${JWT_SECRET:...}`              | HMAC signing key for JWT auth |
| `management.endpoints.web.exposure.include` | `health,info,metrics`   | actuator endpoints |

Run a subgraph with verbose logs:

```bash
java -jar subgraph-property/target/subgraph-property-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=local
```

### Profiles & backend URLs

Each subgraph is wired with four Spring profiles — `local`, `dev`, `stage`,
`prod` — selected via `--spring.profiles.active=<env>` (default: `local`).
Today every profile keeps using the in-memory mock data; the profile blocks
exist so you can drop a real backend URL in once it's available, with no code
change required.

To point a subgraph at a real backend, **either** paste the URL into the
matching profile block in its `src/main/resources/application.yml`:

```yaml
---
spring:
  config:
    activate:
      on-profile: dev
luxe:
  backend:
    base-url: https://api-dev.luxe.example/property
```

**or** set the per-subgraph env var (no code edit) and start with that profile:

```bash
LUXE_PROPERTY_BACKEND_URL=https://api-dev.luxe.example/property \
  java -jar subgraph-property/target/subgraph-property-1.0.0-SNAPSHOT.jar \
       --spring.profiles.active=dev
```

| Subgraph        | Env var                          |
|-----------------|----------------------------------|
| `property`      | `LUXE_PROPERTY_BACKEND_URL`      |
| `guest`         | `LUXE_GUEST_BACKEND_URL`         |
| `pricing`       | `LUXE_PRICING_BACKEND_URL`       |
| `reservations`  | `LUXE_RESERVATIONS_BACKEND_URL`  |
| `loyalty`       | `LUXE_LOYALTY_BACKEND_URL`       |
| `content`       | `LUXE_CONTENT_BACKEND_URL`       |
| `experiences`   | `LUXE_EXPERIENCES_BACKEND_URL`   |
| `meetings`      | `LUXE_MEETINGS_BACKEND_URL`      |
| `notifications` | `LUXE_NOTIFICATIONS_BACKEND_URL` |
| `corporate`     | `LUXE_CORPORATE_BACKEND_URL`     |

The value binds to `LuxeBackendProperties` (`common/config`). When a real
REST data source is added (per-subgraph), it should:

1. Inject `LuxeBackendProperties`.
2. Be annotated `@ConditionalOnProperty(prefix = "luxe.backend", name = "base-url", matchIfMissing = false)`
   so it only loads when the URL is set.
3. Mark the existing `*MockDataSource` with the inverse condition
   (`matchIfMissing = true`) so the mock is the fallback when the URL is empty.

That way `local` always uses mock; `dev`/`stage`/`prod` use the REST data
source if a URL is configured, mock otherwise. No further wiring needed.

### Router

`router/router.yaml` is the local-dev config. Notable settings:

- `supergraph.listen: 0.0.0.0:4000`
- `sandbox.enabled: true` — Apollo Sandbox at the GraphQL endpoint
- `cors.policies` — allows localhost:3000, studio.apollographql.com, luxehotels.com
- `headers.all.request.propagate` — forwards `authorization`, `x-request-id`, `x-correlation-id`
- `include_subgraph_errors.all: true` — surface subgraph errors during dev (turn off in prod)
- `override_subgraph_url` — pins each subgraph's URL to localhost (overrides what's in the supergraph schema)

---

## Authentication

JWT-based, HMAC-signed. The flow:

1. Sign in via `guest.signIn(input: { email, password })` → returns `AuthPayload { token }`.
2. Include `Authorization: Bearer <token>` on subsequent requests.
3. Spring Security is intentionally permissive at the filter layer (`common/auth/SecurityConfig`)
   so Apollo Router's introspection (`_service`, `_entities`) reaches the subgraph unauthenticated.
4. `SubgraphAuthInterceptor` parses the bearer token and stores it on the HTTP request.
5. Resolvers obtain auth via the injected `AuthContextResolver` (production impl
   `DefaultAuthContextResolver` reads from the request; tests substitute a `@Primary`
   bean returning a fixed `AuthContext`). They then call `auth.requireAuth()` or
   `auth.requireRole(AuthRole.PROPERTY_STAFF)` to gate sensitive operations.

Roles: `GUEST < PROPERTY_STAFF < REVENUE_MGR < ADMIN`.

---

## Custom scalars

Registered centrally in `common/src/main/java/com/luxe/common/config/CommonGraphQLConfig.java`:

- ISO-style: `DateTime`, `Date`, `UUID`, `URL`, `CountryCode`, `CurrencyCode`, `LanguageCode`
- Format-validating: `EmailAddress`, `PhoneNumber`
- Domain-specific: `Latitude`, `Longitude`
- `Json` (open-ended object scalar)

`Money` is modeled as an object type (`{ amount: String!, currency: String! }`) rather than
a scalar so clients can select either field. It's marked `@shareable` so all 8 subgraphs
that produce monetary values can return it.

---

## Testing & coverage

The project ships with **635 unit + DGS integration tests** across 35 test classes,
covering common scalars/auth/pagination, every subgraph's mock data source, real
GraphQL execution through `DgsQueryExecutor` (including federation `_entities`
resolution), and authenticated mutation paths via a per-test `AuthContextResolver`
override (see `common/auth/AuthContextResolver.java`).

Test highlights for the most-active subgraphs:
- **Property** (72 tests) — search/sort/facet logic, India IT-corridor seed
  invariants, the destination-autocomplete query (prefix vs substring ranking,
  city/country dedupe with hotel counts), federated `_entities` resolution.
- **Pricing** (58 tests) — FX coverage pin (every currency in
  `PropertyDataGenerator.COUNTRIES` must have an FX entry), explicit
  conversion math (EUR→GBP via USD pivot, OMR→USD above-parity, etc.),
  rate-plan generation for hand-curated and synthetic-rate hotels.

```bash
mvn test                              # full suite (~30s test execution, ~47s with JaCoCo + package)
mvn -pl common test                   # foundation only
mvn -pl subgraph-property test        # one subgraph
mvn -pl common,subgraph-loyalty -am test   # subgraph + its deps
```

Latest per-module breakdown:

| Module        | Instructions | Branches |
|---------------|--------------|----------|
| common        | 59.9%        | 59.7%    |
| property      | 89.8%        | 77.0%    |
| guest         | 80.1%        | 51.1%    |
| pricing       | 84.1%        | 62.3%    |
| reservations  | 83.3%        | 54.1%    |
| loyalty       | 88.1%        | 63.8%    |
| content       | 51.5%        | 24.0%    |
| experiences   | 93.1%        | 71.1%    |
| meetings      | 90.1%        | 65.7%    |
| notifications | 90.1%        | 87.0%    |
| corporate     | 93.9%        | 87.8%    |

JaCoCo is wired in the parent POM (`jacoco-maven-plugin` 0.8.12) and runs
automatically during `mvn test`. Per-module HTML reports land at:

```
{module}/target/site/jacoco/index.html
```

Open one in a browser:

```bash
open subgraph-property/target/site/jacoco/index.html
```

Or grab a quick textual summary across modules:

```bash
for csv in */target/site/jacoco/jacoco.csv; do
  module=${csv%/target*}
  python3 -c "
import csv, sys
i_m=i_c=b_m=b_c=l_m=l_c=0
with open('$csv') as f:
    for row in csv.DictReader(f):
        i_m += int(row['INSTRUCTION_MISSED']); i_c += int(row['INSTRUCTION_COVERED'])
        b_m += int(row['BRANCH_MISSED']); b_c += int(row['BRANCH_COVERED'])
        l_m += int(row['LINE_MISSED']); l_c += int(row['LINE_COVERED'])
print(f'$module instr={100*i_c/(i_c+i_m):.1f}% branch={100*b_c/(b_c+b_m):.1f}% line={100*l_c/(l_c+l_m):.1f}%')"
done
```

Excluded from coverage:
- `*Application.class` — Spring Boot bootstrap, no logic
- Federation reference stub classes (e.g. `subgraph-experiences` Hotel.class) — id-only POJOs

---

## Federation patterns demonstrated

- **Entity ownership** — each entity has a single owning subgraph (e.g. `Hotel` lives in `property`).
- **Type extensions** — other subgraphs add fields via `type Hotel @key(fields: "id") { id: ID! ... }`
  and a pass-through `@DgsEntityFetcher`.
- **`@shareable` value/error types** — `PageInfo`, `NotFoundError`, `ValidationError`, `Money`,
  `LocalizedContent`, etc. defined in multiple subgraphs with identical shapes.
- **Reference stub classes** — each non-owning subgraph has a small `Hotel` / `GuestProfile`
  / `RoomType` Java class with just `id` so DGS can map entity-fetcher returns to the
  correct GraphQL type.
- **Type-name disambiguation** — when two subgraphs needed semantically-different types with
  the same name, we renamed (e.g. property's `MediaAsset` → `PropertyMediaAsset`,
  property's `LocalizedContent` → `PropertyLocalizedContent`,
  property's `CancellationPolicy` → `HotelCancellationPolicy`).

---

## Adding a new subgraph

1. Copy an existing `subgraph-X/` directory and rename to `subgraph-newname/`.
2. Update package names in Java source (`com.luxe.X` → `com.luxe.newname`).
3. Pick a port (next free in 4011+); update `application.yml` and `Dockerfile`.
4. Add the module to the parent `pom.xml`'s `<modules>` block.
5. Add an entry to `supergraph.yaml` pointing at the new schema file.
6. Add an `override_subgraph_url` entry in `router/router.yaml`.
7. Add a launch entry to `scripts/start-subgraphs.sh`.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Subgraph fails to start with "scalar X not found" | A new schema declared a custom scalar without registering its coercer in `CommonGraphQLConfig`. |
| Subgraph fails with "tried to redefine existing type" | Stale `target/classes` — run `mvn clean package` instead of `mvn package`. |
| Federation composition fails with `INVALID_FIELD_SHARING` | A type defined in multiple subgraphs is missing `@shareable`. |
| Federation composition fails with `FIELD_TYPE_MISMATCH` | Same type name with different shapes across subgraphs — rename one or align fields. |
| Composition fails with `SATISFIABILITY_ERROR` re: `@key` | An entity stub (`type X @key(fields: "id") { id: ID! @external }`) — drop `@external` from key fields under Federation 2 conventions. |
| Router returns 401 on `_service { sdl }` | Spring Security blocking introspection — check `common/auth/SecurityConfig` is wired. |
| Router returns "Subgraph errors redacted" | Set `include_subgraph_errors.all: true` in `router.yaml` to see real errors during dev. |
| Apollo IntelliJ plugin warns "type definitions must be in `.graphqls`" | Schema files use `.graphql`. Rename to `.graphqls` (DGS auto-discovers both). |

---

## Reference REST integration: content subgraph

A working REST data source ships for the **content** subgraph as the template
to copy when other backends become available. The companion backend service
lives at [`../luxe-hotels-content-api/`](../luxe-hotels-content-api) (Spring
Boot, port `7006`). It serves the same article / inspiration / brand-story /
spotlight / collection data the mock has, as flat JSON DTOs.

### Pattern

`subgraph-content` ships **two** `ContentDataSource` implementations selected
at startup by a SpEL expression on `luxe.backend.base-url`:

| URL value           | Loaded data source         | Annotation                                                                |
|---------------------|----------------------------|---------------------------------------------------------------------------|
| empty / unset       | `ContentMockDataSource`    | `@ConditionalOnExpression("'${luxe.backend.base-url:}'.length() == 0")`   |
| any non-empty value | `ContentRestDataSource`    | `@ConditionalOnExpression("'${luxe.backend.base-url:}'.length() > 0")`    |

The REST data source uses Spring 6 `RestClient`, decodes flat record DTOs
(`RestDtos`), and maps them to the existing `Article` / `TravelInspiration`
domain classes — so resolvers and the GraphQL schema are unchanged.

### Try it locally

```bash
# 1. Start the content backend
cd ../luxe-hotels-content-api
mvn -q package -DskipTests
java -jar target/luxe-hotels-content-api-1.0.0-SNAPSHOT.jar &
# health: http://localhost:7006/actuator/health

# 2. Start the content subgraph in dev mode pointing at the backend
cd ../luxe-hotels-graphqlwithJava
LUXE_CONTENT_BACKEND_URL=http://localhost:7006 \
  java -jar subgraph-content/target/subgraph-content-1.0.0-SNAPSHOT.jar \
       --spring.profiles.active=dev
```

A query against `http://localhost:4006/graphql` now flows: GraphQL →
`ContentRestDataSource` → backend JSON → DTO mapper → domain types → GraphQL
response. Drop `LUXE_CONTENT_BACKEND_URL` (or run with `--spring.profiles.active=local`)
to fall back to the in-memory mock.

### Replicating for other subgraphs

Per subgraph (when the real backend exists):

1. Define a wire-format `RestDtos` record class.
2. Add `XRestDataSource implements XDataSource` annotated
   `@ConditionalOnExpression("'${luxe.backend.base-url:}'.length() > 0")`.
3. Add the inverse condition (`length() == 0`) to the existing `XMockDataSource`.
4. The subgraph's `application.yml` already has `luxe.backend.base-url` and
   per-env profile blocks ready — no config changes needed.

---

## Roadmap / next steps

In rough effort-vs-payoff order:

1. **Replicate the REST pattern** for the remaining subgraphs once their
   backends are available. Content is the worked example above.
2. **CI** — GitHub Actions running `mvn verify` on PRs, with the JaCoCo
   summary published as a check.
3. **Fix composition hints** — align nullability of `ValidationError.fieldErrors`
   (`[FieldError!]` in property vs `[FieldError!]!` everywhere else) and
   `NotFoundError.resourceType` so `rover supergraph compose` is hint-free.
4. **Lift `content` / `guest` branch coverage** — they sit at 24% / 51%, the
   lowest in the matrix. Mostly untested validation branches, pagination
   edge cases, and the REST-data-source error paths in content.
5. **Idempotency enforcement** — every mutation already accepts an
   `idempotencyKey: UUID!`, but no subgraph currently dedupes against a replay
   store. Add a simple in-memory cache keyed by `(mutationName, key)` per
   subgraph.
6. **Persistence layer** — swap the in-memory mocks for H2 + Spring Data JPA
   (or Postgres in `dev`+). Closer to production without depending on the
   real backends being ready.
7. **Observability** — OpenTelemetry traces propagated from router → subgraph,
   structured JSON logs, Prometheus `/metrics` exposure on every subgraph.
8. **Deployment** — flesh out `docker-compose.yml` for one-command local
   stack; add a Helm chart for K8s.

---

## Tech stack

- Java 21 (records, virtual threads)
- Spring Boot 3.3.4
- Netflix DGS 8.7.1 (`graphql-dgs-spring-graphql-starter`)
- graphql-java extended scalars 21.0
- Apollo Federation 2.9
- Apollo Router 2.14
- JJWT 0.12.6 for JWT signing
- Maven 3.9 multi-module reactor
