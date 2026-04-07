package com.placementpro.backend.security;

import com.placementpro.backend.exception.ErrorResponse;
import com.placementpro.backend.service.IpRateLimiter;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Sensitive endpoint rate limiting filter
 *
 * Enforces stricter rate limits on sensitive authentication endpoints:
 * - POST /api/auth/login (5 attempts per 15 minutes, tracked separately)
 * - POST /api/auth/register (20 requests per minute per IP)
 * - POST /api/auth/forgot-password (20 requests per minute per IP)
 * - POST /api/auth/reset-password (20 requests per minute per IP)
 *
 * Returns 429 Too Many Requests with standardized ErrorResponse format.
 */
@Slf4j
public class SensitiveEndpointRateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private IpRateLimiter ipRateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Sensitive endpoints with stricter rate limiting
    private static final String[] SENSITIVE_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if this is a sensitive endpoint that requires stricter rate limiting
        if (!isSensitiveEndpoint(request.getRequestURI(), request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        // Check sensitive endpoint rate limit (stricter: 20 per minute)
        ConsumptionProbe probe = ipRateLimiter.checkSensitiveEndpointLimit(clientIp);

        if (!probe.isConsumed()) {
            // Rate limit exceeded
            long retryAfter = ipRateLimiter.getRetryAfter(probe);

            // Log abuse detection event
            String requestId = (String) request.getAttribute("requestId");
            String path = request.getRequestURI();

            log.warn("RATE_LIMIT_EXCEEDED ip={} endpoint={} requestId={} type=SENSITIVE_ENDPOINT_LIMIT retryAfter={}s",
                    clientIp, path, requestId, retryAfter);

            // Build standardized error response
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Too many requests to this endpoint. Please try again later.")
                    .path(path)
                    .requestId(requestId)
                    .build();

            // Send 429 response with Retry-After header
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.getWriter().flush();
            return;
        }

        // Rate limit not exceeded, continue with filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request is to a sensitive endpoint
     */
    private boolean isSensitiveEndpoint(String requestUri, String method) {
        for (String endpoint : SENSITIVE_ENDPOINTS) {
            if (requestUri.startsWith(endpoint) && "POST".equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract client IP address from request
     * Supports X-Forwarded-For header for proxied requests
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip this filter for non-sensitive paths
        return !isSensitiveEndpoint(request.getRequestURI(), request.getMethod());
    }
}
