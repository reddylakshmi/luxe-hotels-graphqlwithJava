package com.luxe.experiences.config;

import com.luxe.common.auth.SubgraphAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ExperiencesGraphQLConfig implements WebMvcConfigurer {
    private final SubgraphAuthInterceptor authInterceptor;
    public ExperiencesGraphQLConfig(SubgraphAuthInterceptor authInterceptor) { this.authInterceptor = authInterceptor; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/graphql");
    }
}
