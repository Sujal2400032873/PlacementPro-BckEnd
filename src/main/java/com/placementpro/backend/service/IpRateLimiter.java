package com.placementpro.backend.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.placementpro.backend.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * IP-based rate limiter service using Bucket4j
 *
 * Manages rate limiting buckets per IP address with automatic cache expiration.
 * Thread-safe caching of buckets to avoid memory leaks and ensure concurrent request handling.
 */
@Slf4j
@Service
public class IpRateLimiter {

    private final RateLimitConfig rateLimitConfig;
    private final Cache<String, Bucket> globalApiCache;
    private final Cache<String, Bucket> sensitiveEndpointCache;

    // Cache expiration time: 30 minutes (provides some buffer beyond the longest limit of 15 min)
    private static final long CACHE_EXPIRATION_MINUTES = 30;

    public IpRateLimiter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;

        this.globalApiCache = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .build();

        this.sensitiveEndpointCache = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Check global API rate limit for given IP
     *
     * @param ipAddress the client IP address
     * @return ConsumptionProbe indicating if the request is allowed
     */
    public ConsumptionProbe checkGlobalApiLimit(String ipAddress) {
        Bucket bucket = globalApiCache.getIfPresent(ipAddress);
        if (bucket == null) {
            bucket = rateLimitConfig.createGlobalApiBucket();
            globalApiCache.put(ipAddress, bucket);
        }
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Check sensitive endpoint rate limit for given IP
     *
     * @param ipAddress the client IP address
     * @return ConsumptionProbe indicating if the request is allowed
     */
    public ConsumptionProbe checkSensitiveEndpointLimit(String ipAddress) {
        Bucket bucket = sensitiveEndpointCache.getIfPresent(ipAddress);
        if (bucket == null) {
            bucket = rateLimitConfig.createSensitiveEndpointBucket();
            sensitiveEndpointCache.put(ipAddress, bucket);
        }
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Get retry-after duration in seconds
     *
     * @param probe the ConsumptionProbe from tryConsumeToken()
     * @return retry-after value in seconds
     */
    public long getRetryAfter(ConsumptionProbe probe) {
        long nanosToWait = probe.getNanosToWaitForRefill();
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToWait));
    }
}
