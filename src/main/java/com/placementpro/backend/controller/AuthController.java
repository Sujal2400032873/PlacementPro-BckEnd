package com.placementpro.backend.controller;

import com.placementpro.backend.dto.*;
import com.placementpro.backend.service.AuthService;
import com.placementpro.backend.service.IpRateLimiter;
import com.placementpro.backend.exception.RateLimitExceededException;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private IpRateLimiter ipRateLimiter;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        AuthResponse response = authService.login(loginRequest);

        boolean secureCookie = shouldUseSecureCookie(request);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("placementpro_token", response.getToken())
                .httpOnly(true)
            .secure(secureCookie)
                .path("/")
                .maxAge(24 * 60 * 60)
            .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain)) {
            cookieBuilder.domain(cookieDomain.trim());
        }

        ResponseCookie cookie = cookieBuilder.build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response.getUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("placementpro_token", "")
                .httpOnly(true)
                .secure(shouldUseSecureCookie(request))
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain)) {
            cookieBuilder.domain(cookieDomain.trim());
        }

        ResponseCookie cookie = cookieBuilder.build();

        log.info("LOGOUT_SUCCESS");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        UserDTO user = authService.getAuthenticatedUser(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest, HttpServletRequest request) {
        enforceSensitiveEndpointLimit(request, "/api/auth/register");
        MessageResponse response = authService.register(signUpRequest);
        if (response.getMessage().contains("Error")) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email, HttpServletRequest request) {
        enforceSensitiveEndpointLimit(request, "/api/auth/forgot-password");
        MessageResponse response = authService.forgotPassword(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword, HttpServletRequest request) {
        enforceSensitiveEndpointLimit(request, "/api/auth/reset-password");
        MessageResponse response = authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(response);
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

    private void enforceSensitiveEndpointLimit(HttpServletRequest request, String endpoint) {
        String clientIp = getClientIp(request);
        ConsumptionProbe probe = ipRateLimiter.checkSensitiveEndpointLimit(clientIp);

        if (!probe.isConsumed()) {
            long retryAfter = ipRateLimiter.getRetryAfter(probe);
            String requestId = (String) request.getAttribute("requestId");
            log.warn("RATE_LIMIT_EXCEEDED ip={} endpoint={} type=SENSITIVE_ENDPOINT_LIMIT requestId={} retryAfter={}s",
                    clientIp, endpoint, requestId, retryAfter);

            throw new RateLimitExceededException(
                    "Too many requests. Please try again later.",
                    clientIp,
                    retryAfter,
                    "SENSITIVE_ENDPOINT"
            );
        }
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        if (cookieSecure) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && forwardedProto.equalsIgnoreCase("https")) {
            return true;
        }

        return request.isSecure();
    }
}
