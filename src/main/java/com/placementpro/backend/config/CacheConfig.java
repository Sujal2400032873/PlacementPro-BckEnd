package com.placementpro.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        CaffeineCache adminDashboardCache = new CaffeineCache(
                "adminDashboard",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
        );

        CaffeineCache officerDashboardCache = new CaffeineCache(
                "officerDashboard",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
        );

        cacheManager.setCaches(List.of(adminDashboardCache, officerDashboardCache));
        return cacheManager;
    }
}
