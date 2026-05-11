package com.luxe.common.auth;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.schema.idl.RuntimeWiring;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-wires the {@code @auth} directive into every subgraph that
 * pulls in this {@code common} module. A subgraph activates the
 * directive simply by:
 *
 * <ol>
 *   <li>declaring it in {@code schema.graphqls} (along with the
 *       {@code AuthRole} enum) — see
 *       {@code subgraph-guest/.../schema.graphqls} for the
 *       canonical declaration;</li>
 *   <li>applying {@code @auth(requires: ROLE)} to any sensitive
 *       field.</li>
 * </ol>
 *
 * <p>Subgraphs that never use the directive can leave their schema
 * unchanged — registering the directive wiring without a
 * corresponding schema declaration is a no-op (graphql-java only
 * invokes the wiring for fields that carry the directive).
 */
@Configuration
@DgsComponent
public class AuthDirectiveConfig {

    private final ObjectProvider<AuthContextResolver> resolvers;

    public AuthDirectiveConfig(ObjectProvider<AuthContextResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @DgsRuntimeWiring
    public RuntimeWiring.Builder addAuthDirective(RuntimeWiring.Builder builder) {
        // Register by name (not via the generic directiveWiring) so the
        // wiring only fires for fields actually carrying @auth — the
        // generic registration would fire on every field with *any*
        // directive (@key, @shareable, etc.) and try to look up the
        // missing @auth applied directive, NPE-ing on a null lookup.
        return builder.directive("auth", new AuthDirectiveWiring(resolvers));
    }
}
