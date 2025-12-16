package com.bark.twitter.config;

import com.bark.twitter.exception.ForbiddenException;
import com.bark.twitter.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "x-api-key";

    private final SecurityConfig securityConfig;

    public ApiKeyInterceptor(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // Allow public endpoints without authentication
        if (path.equals("/health") ||
            path.startsWith("/actuator") ||
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

        return true;
    }
}
