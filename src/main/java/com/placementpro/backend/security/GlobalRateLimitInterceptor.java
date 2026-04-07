package com.placementpro.backend.security;

import com.placementpro.backend.exception.RateLimitExceededException;
import com.placementpro.backend.service.IpRateLimiter;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Global API rate limiting interceptor.
 * Enforces 100 requests/minute per IP for general API endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRateLimitInterceptor implements HandlerInterceptor {

    private final IpRateLimiter ipRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Exclude auth-sensitive endpoints because they have dedicated limits.
        if (path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/health")) {
            return true;
        }

        String clientIp = getClientIp(request);
        ConsumptionProbe probe = ipRateLimiter.checkGlobalApiLimit(clientIp);

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfter = ipRateLimiter.getRetryAfter(probe);
        String requestId = (String) request.getAttribute("requestId");

        log.warn("RATE_LIMIT_EXCEEDED ip={} endpoint={} requestId={} type=GLOBAL_API_LIMIT retryAfter={}s",
                clientIp, path, requestId, retryAfter);

        throw new RateLimitExceededException(
            "Too many requests. Please try again later.",
            clientIp,
            retryAfter,
            "GLOBAL_API"
        );
    }

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
}
