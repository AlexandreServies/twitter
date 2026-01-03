package com.bark.twitter.config;

import com.bark.twitter.credits.CreditService;
import com.bark.twitter.exception.ForbiddenException;
import com.bark.twitter.exception.NoCreditsException;
import com.bark.twitter.exception.UnauthorizedException;
import com.bark.twitter.usage.UsageTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "x-api-key";
    public static final String API_KEY_ATTRIBUTE = "apiKey";

    private final SecurityConfig securityConfig;
    private final UsageTrackingService usageTrackingService;
    private final CreditService creditService;

    public ApiKeyInterceptor(SecurityConfig securityConfig, UsageTrackingService usageTrackingService, CreditService creditService) {
        this.securityConfig = securityConfig;
        this.usageTrackingService = usageTrackingService;
        this.creditService = creditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // Allow public endpoints without authentication
        if (path.equals("/health") ||
            path.equals("/actuator/health") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.equals("/swagger-ui.html")) {
            return true;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Missing x-api-key header");
        }

        if (!securityConfig.isValidApiKey(apiKey)) {
            throw new ForbiddenException("Invalid API key");
        }

        // Store API key in request for /usage endpoint access
        request.setAttribute(API_KEY_ATTRIBUTE, apiKey);

        // Track usage asynchronously - zero latency impact (only known endpoints)
        String endpoint = normalizeEndpoint(path);
        if (endpoint != null) {
            // Check and decrement credits
            if (!creditService.decrementCredit(apiKey)) {
                throw new NoCreditsException("No credits remaining");
            }

            usageTrackingService.recordCall(apiKey, endpoint);
        }

        return true;
    }

    /**
     * Normalizes endpoint path to group by base endpoint.
     * e.g., /tweet/123 -> /tweet, /user/456 -> /user
     * Only tracks known endpoints, returns null for unknown.
     */
    private String normalizeEndpoint(String path) {
        // Normalize any double slashes
        path = path.replaceAll("//+", "/");

        if (path.startsWith("/tweet/")) return "/tweet";
        if (path.startsWith("/user/")) return "/user";
        if (path.startsWith("/community/")) return "/community";

        // Return null for unknown endpoints (won't be tracked)
        return null;
    }
}
