package com.luxe.common.auth;

import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Production resolver: reads the {@link AuthContext} that
 * {@link SubgraphAuthInterceptor} attached to the inbound HTTP request.
 *
 * <p>Lookup strategy (first hit wins):
 * <ol>
 *   <li>{@code dfe.getGraphQlContext().get(HttpServletRequest.class)} —
 *       works when callers explicitly stash the request under that key.</li>
 *   <li>Spring's {@link RequestContextHolder} — populated for every MVC
 *       request thread by {@code RequestContextFilter}, which is what
 *       Spring Boot wires by default. This is the path the federated
 *       runtime actually uses (DGS + Spring GraphQL doesn't put the raw
 *       servlet request into the GraphQL context).</li>
 * </ol>
 *
 * Tests can still override this whole bean with a {@code @TestConfiguration}
 * that returns a fixed {@link AuthContext} per the existing pattern in
 * {@code GuestAuthenticatedTest}.
 */
@Component
public class DefaultAuthContextResolver implements AuthContextResolver {

    private static final String ATTRIBUTE = "authContext";

    @Override
    public AuthContext resolve(DataFetchingEnvironment dfe) {
        try {
            HttpServletRequest req = dfe.getGraphQlContext().get(HttpServletRequest.class);
            if (req == null) req = currentServletRequest();
            if (req == null) return AuthContext.anonymous();
            AuthContext ctx = (AuthContext) req.getAttribute(ATTRIBUTE);
            return ctx != null ? ctx : AuthContext.anonymous();
        } catch (Exception e) {
            return AuthContext.anonymous();
        }
    }

    private static HttpServletRequest currentServletRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sa) {
            return sa.getRequest();
        }
        return null;
    }
}
