package com.placementpro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.frontend.origins:http://localhost:5173}")
    private String frontendOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(
                CorsRegistry registry
            ) {

                registry.addMapping("/**")
                    .allowedOriginPatterns(Arrays.stream(frontendOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new))
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true);

            }

        };

    }
}