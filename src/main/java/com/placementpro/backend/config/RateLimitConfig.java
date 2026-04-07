package com.placementpro.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Configuration using Bucket4j
 *
 * Provides bucket creations for different rate limiting scenarios:
 * - Global API rate limiting (100 requests per minute per IP)
 * - Endpoint-specific throttling for sensitive endpoints
 */
@Configuration
public class RateLimitConfig {

    public int getGlobalApiLimit() {
        return 100; // Configurable via rate-limit.global-api.limit
    }

    public int getSensitiveEndpointLimit() {
        return 20; // Configurable via rate-limit.sensitive-endpoint.limit
    }

    /**
     * Global API bucket: 100 requests per minute per IP (configurable)
     */
    public Bucket createGlobalApiBucket() {
        Bandwidth limit = Bandwidth.classic(getGlobalApiLimit(), 
            Refill.intervally(getGlobalApiLimit(), Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Sensitive endpoints bucket: 20 requests per minute per IP (configurable)
     * (stricter than global limit for endpoints like /register, /forgot-password, /reset-password)
     */
    public Bucket createSensitiveEndpointBucket() {
        Bandwidth limit = Bandwidth.classic(getSensitiveEndpointLimit(), 
            Refill.intervally(getSensitiveEndpointLimit(), Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Calculate retry-after value in seconds based on remaining duration
     */
    public long calculateRetryAfter(long nanosDuration) {
        return TimeUnit.NANOSECONDS.toSeconds(nanosDuration) + 1;
    }
}
