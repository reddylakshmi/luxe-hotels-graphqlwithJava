package com.luxe.common.auth;

import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Production resolver: reads the auth context that {@link SubgraphAuthInterceptor}
 * attached to the inbound HTTP request as an attribute.
 */
@Component
public class DefaultAuthContextResolver implements AuthContextResolver {

    private static final String ATTRIBUTE = "authContext";

    @Override
    public AuthContext resolve(DataFetchingEnvironment dfe) {
        try {
            HttpServletRequest req = dfe.getGraphQlContext().get(HttpServletRequest.class);
            if (req == null) return AuthContext.anonymous();
            AuthContext ctx = (AuthContext) req.getAttribute(ATTRIBUTE);
            return ctx != null ? ctx : AuthContext.anonymous();
        } catch (Exception e) {
            return AuthContext.anonymous();
        }
    }
}
