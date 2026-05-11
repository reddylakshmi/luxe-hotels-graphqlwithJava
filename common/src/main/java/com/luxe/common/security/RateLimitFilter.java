package com.luxe.common.security;

import com.luxe.common.auth.AuthContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request rate-limit gate. Picks a bucket key based on the
 * inbound principal:
 * <ul>
 *   <li>Authenticated requests bucket by {@code user:<guestId>} —
 *       so a logged-in scraper can't open a thousand IPs and pretend
 *       to be many users.</li>
 *   <li>Anonymous requests bucket by {@code ip:<x-forwarded-for|remote>}
 *       — limits unauthenticated bursts (CAPTCHA-style behavior).</li>
 * </ul>
 *
 * <p>The {@link AuthContext} attribute is set by
 * {@code SubgraphAuthInterceptor} earlier in the filter chain (or by
 * the gateway-validated headers in the router-trust path — see C3
 * docs). Either way, when the bucket is exhausted we write a 429
 * with a structured body so the client can backoff intelligently.
 *
 * <p>Disabled by setting {@code luxe.security.rate-limit.enabled=false}
 * — useful for the test runner and for any subgraph that's behind
 * its own dedicated rate limiter.
 */
@Component
@ConditionalOnProperty(prefix = "luxe.security.rate-limit", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitStore store;
    private final boolean enabled;

    public RateLimitFilter(
            RateLimitStore store,
            @Value("${luxe.security.rate-limit.enabled:true}") boolean enabled) {
        this.store = store;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        // GraphiQL + actuator probes skip the limiter — they're
        // operator surfaces, not anonymous ingress.
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/actuator") || path.startsWith("/graphiql"))) {
            chain.doFilter(request, response);
            return;
        }

        String key = bucketKey(request);
        if (!store.tryConsume(key)) {
            log.warn("rate-limit exceeded for {}", key);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"errors\":[{\"message\":\"Rate limit exceeded\","
                            + "\"extensions\":{\"code\":\"RATE_LIMITED\"}}]}");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Build the bucket key for this request. Authenticated requests
     * use the guest id (already populated as a request attribute by
     * SubgraphAuthInterceptor); anonymous requests fall back to the
     * client IP, honoring {@code X-Forwarded-For} since the router
     * sits in front.
     */
    private String bucketKey(HttpServletRequest request) {
        AuthContext ctx = (AuthContext) request.getAttribute("authContext");
        if (ctx != null && ctx.isAuthenticated() && ctx.guestId() != null) {
            return "user:" + ctx.guestId();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First entry is the originating client; the rest are
            // intermediate proxies.
            int comma = forwarded.indexOf(',');
            return "ip:" + (comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim());
        }
        return "ip:" + request.getRemoteAddr();
    }
}
