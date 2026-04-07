package com.placementpro.backend.config;

import com.placementpro.backend.security.GlobalRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * NOTE: CORS is fully handled by SecurityConfig.corsConfigurationSource()
 * which is registered directly with Spring Security's filter chain.
 * No MVC-level CORS config needed here to avoid duplication.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final GlobalRateLimitInterceptor globalRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalRateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    // CORS is handled by SecurityConfig's CorsConfigurationSource bean.
    // This class is kept for future MVC-level configurations (view resolvers, formatters, etc.)
}
