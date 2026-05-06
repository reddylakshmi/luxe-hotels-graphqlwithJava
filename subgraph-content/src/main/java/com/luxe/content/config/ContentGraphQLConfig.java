package com.luxe.content.config;

import com.luxe.common.auth.SubgraphAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ContentGraphQLConfig implements WebMvcConfigurer {
    private final SubgraphAuthInterceptor authInterceptor;
    public ContentGraphQLConfig(SubgraphAuthInterceptor authInterceptor) { this.authInterceptor = authInterceptor; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/graphql");
    }
}
