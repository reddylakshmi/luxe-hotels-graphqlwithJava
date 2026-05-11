package com.luxe.common.auth;

import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Field-level authorization via the {@code @auth(requires: AuthRole!)}
 * directive. Wraps the default {@link DataFetcher} so the configured
 * {@link AuthRole} is checked before the underlying resolver runs.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Anonymous request on a directive-protected field → throw
 *       {@link UnauthorizedException} (surfaces as {@code UNAUTHORIZED}
 *       in the GraphQL error extensions).</li>
 *   <li>Authenticated request with insufficient role → throw
 *       {@link ForbiddenException} (surfaces as {@code FORBIDDEN}).</li>
 *   <li>Authenticated request with sufficient role → delegate to the
 *       original fetcher.</li>
 * </ul>
 *
 * <p>The {@link AuthRole} hierarchy is ordinal-based
 * (GUEST &lt; LOYALTY_MEMBER &lt; PROPERTY_STAFF &lt; REVENUE_MGR &lt; ADMIN),
 * so {@code @auth(requires: GUEST)} on a field is satisfied by any
 * authenticated principal — including a property staff token. That
 * matches the "any logged-in human can see their own data" pattern
 * the booking/account surfaces want.
 *
 * <p>Row-level ownership (e.g. "guest can only see <em>their own</em>
 * reservations") is intentionally <strong>not</strong> enforced here —
 * the directive only knows the field name, not the parent entity's
 * owner. That stays the resolver's job, via {@code auth.guestId()}
 * comparisons. The directive is the role gate; the resolver is the
 * tenancy gate. Two layers, separately auditable.
 *
 * <p>{@link AuthContextResolver} is injected as an {@link ObjectProvider}
 * so the wiring also works in subgraphs that haven't wired auth at all
 * (cheap defense-in-depth — if the bean is missing we treat every
 * request as anonymous, never accidentally permissive).
 */
public class AuthDirectiveWiring implements SchemaDirectiveWiring {

    private static final Logger log = LoggerFactory.getLogger(AuthDirectiveWiring.class);

    private final ObjectProvider<AuthContextResolver> resolvers;

    public AuthDirectiveWiring(ObjectProvider<AuthContextResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public GraphQLFieldDefinition onField(
            SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
        AuthRole required = parseRequired(env);
        GraphQLFieldDefinition field = env.getElement();
        FieldCoordinates coords =
                FieldCoordinates.coordinates(env.getFieldsContainer().getName(), field.getName());
        DataFetcher<?> original = env.getCodeRegistry().getDataFetcher(coords, field);

        DataFetcher<?> guarded = dfe -> {
            AuthContextResolver resolver = resolvers.getIfAvailable();
            AuthContext ctx = resolver != null ? resolver.resolve(dfe) : AuthContext.anonymous();
            if (!ctx.isAuthenticated()) {
                // Return a DataFetcherResult with the error attached
                // rather than throwing, so graphql-java preserves the
                // GraphQLError extensions (code=UNAUTHORIZED) instead
                // of wrapping in a generic ExceptionWhileDataFetching.
                return DataFetcherResult.newResult()
                        .error(new UnauthorizedException(
                                "Authentication required for field '" + field.getName() + "'"))
                        .build();
            }
            if (!ctx.hasRole(required)) {
                return DataFetcherResult.newResult()
                        .error(new ForbiddenException(
                                "Role " + required + " required for field '"
                                        + field.getName() + "', principal has " + ctx.role()))
                        .build();
            }
            return original.get(dfe);
        };

        env.getCodeRegistry().dataFetcher(coords, guarded);
        return field;
    }

    /**
     * Parse the {@code requires} argument off the {@code @auth}
     * directive. The schema declares it as an enum, so graphql-java
     * hands us either a {@link String} (most common) or an
     * {@link Enum} depending on coercion mode. Both shapes are
     * accepted, and unknown values fall back to the strictest role
     * (ADMIN) — fail-closed rather than silently treating a typo as
     * "no protection required".
     */
    private static AuthRole parseRequired(
            SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
        Object raw = env.getAppliedDirective("auth").getArgument("requires").getValue();
        if (raw == null) {
            log.warn("@auth on '{}' has no 'requires' argument — defaulting to ADMIN (fail-closed)",
                    env.getElement().getName());
            return AuthRole.ADMIN;
        }
        try {
            if (raw instanceof AuthRole ar) return ar;
            return AuthRole.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            log.warn("@auth on '{}' has unknown role '{}' — defaulting to ADMIN (fail-closed)",
                    env.getElement().getName(), raw);
            return AuthRole.ADMIN;
        }
    }

    /** Marker subclass so dashboards can distinguish 401 vs 403 in extensions.code. */
    public static final class ForbiddenException extends GraphQLException
            implements graphql.GraphQLError {

        public ForbiddenException(String message) {
            super(message);
        }

        @Override
        public java.util.List<graphql.language.SourceLocation> getLocations() {
            return null;
        }

        @Override
        public graphql.ErrorClassification getErrorType() {
            return graphql.ErrorType.ValidationError;
        }

        @Override
        public java.util.Map<String, Object> getExtensions() {
            return java.util.Map.of("code", "FORBIDDEN");
        }
    }
}
