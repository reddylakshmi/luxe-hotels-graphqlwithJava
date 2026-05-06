package com.luxe.meetings.config;

import com.luxe.common.auth.SubgraphAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MeetingsGraphQLConfig implements WebMvcConfigurer {
    private final SubgraphAuthInterceptor authInterceptor;
    public MeetingsGraphQLConfig(SubgraphAuthInterceptor authInterceptor) { this.authInterceptor = authInterceptor; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/graphql");
    }
}
