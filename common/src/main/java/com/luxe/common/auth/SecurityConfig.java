package com.luxe.common.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permits all HTTP requests at the Spring Security layer. Authentication is enforced
 * inside resolvers via {@link SubgraphAuthInterceptor} populating an {@link AuthContext}
 * from the Bearer token, and {@code AuthContext.requireAuth()} / {@code requireRole(...)}
 * calls. Apollo Federation operations (_service, _entities) must be unauthenticated so
 * the supergraph router can discover the schema and resolve cross-subgraph entity refs.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
