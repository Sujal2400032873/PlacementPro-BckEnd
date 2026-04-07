package com.placementpro.backend.exception;

/**
 * Exception thrown when a rate limit is exceeded
 * Handled by GlobalExceptionHandler to return 429 Too Many Requests
 */
public class RateLimitExceededException extends RuntimeException {

    private final String ipAddress;
    private final long retryAfterSeconds;
    private final String limitType; // e.g., "LOGIN", "GLOBAL_API", "SENSITIVE_ENDPOINT"

    public RateLimitExceededException(String message, String ipAddress, long retryAfterSeconds, String limitType) {
        super(message);
        this.ipAddress = ipAddress;
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitType = limitType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getLimitType() {
        return limitType;
    }
}
