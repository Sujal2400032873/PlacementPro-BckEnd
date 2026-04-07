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
 * Global API rate limiting filter
 *
 * Enforces global rate limit of 100 requests per minute per IP across all endpoints.
 * Runs after RequestIdFilter to ensure requestId is available in MDC.
 * Returns 429 Too Many Requests with standardized ErrorResponse format.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private IpRateLimiter ipRateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // List of endpoints excluded from global rate limiting (login/register have their own limits)
    private static final String[] EXCLUDED_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/health",
            "/api/public"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if this path should be excluded from global rate limiting
        if (isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        // Check global API rate limit
        ConsumptionProbe probe = ipRateLimiter.checkGlobalApiLimit(clientIp);

        if (!probe.isConsumed()) {
            // Rate limit exceeded
            long retryAfter = ipRateLimiter.getRetryAfter(probe);

            // Log abuse detection event
            String requestId = (String) request.getAttribute("requestId");
            String path = request.getRequestURI();

            log.warn("RATE_LIMIT_EXCEEDED ip={} endpoint={} requestId={} type=GLOBAL_API_LIMIT retryAfter={}s",
                    clientIp, path, requestId, retryAfter);

            // Build standardized error response
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Too many requests. Please try again later.")
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
     * Check if the request path should be excluded from global rate limiting
     */
    private boolean isExcludedPath(String requestUri) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestUri.startsWith(excludedPath)) {
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
            // X-Forwarded-For can contain multiple IPs; take the first one
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
        // Skip this filter for excluded paths
        return isExcludedPath(request.getRequestURI());
    }
}
