package com.luxe.common.auth;

import graphql.schema.DataFetchingEnvironment;

/**
 * Resolves the per-request {@link AuthContext} from the GraphQL execution environment.
 *
 * <p>The default Spring bean ({@link DefaultAuthContextResolver}) reads the auth
 * context that {@link SubgraphAuthInterceptor} attached to the inbound HTTP request.
 * Tests can supply a {@code @Primary} bean (typically a lambda) to inject a known
 * principal — useful because {@code DgsQueryExecutor} bypasses Spring MVC, so the
 * interceptor never runs.
 */
@FunctionalInterface
public interface AuthContextResolver {
    AuthContext resolve(DataFetchingEnvironment dfe);
}
