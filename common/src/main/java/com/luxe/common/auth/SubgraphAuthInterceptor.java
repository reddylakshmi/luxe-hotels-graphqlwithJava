package com.luxe.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SubgraphAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SubgraphAuthInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_CONTEXT_ATTR = "authContext";

    private final JwtService jwtService;

    public SubgraphAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            AuthContext ctx = jwtService.parseToken(token);
            request.setAttribute(AUTH_CONTEXT_ATTR, ctx);
            log.debug("Auth context set for guest: {}", ctx.guestId());
        } else {
            request.setAttribute(AUTH_CONTEXT_ATTR, AuthContext.anonymous());
        }

        return true;
    }
}
