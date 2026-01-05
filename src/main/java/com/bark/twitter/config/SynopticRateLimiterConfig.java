package com.bark.twitter.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for Synoptic API rate limiting.
 * Provides separate rate limiters per endpoint type (40 calls/second each).
 */
@Configuration
public class SynopticRateLimiterConfig {

    private static final int CALLS_PER_SECOND = 40;

    /**
     * Rate limiter for users-by-id batch endpoint.
     */
    @Bean
    public RateLimiter synopticUsersByIdRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(CALLS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMinutes(2)) // Wait up to 2 min for permit
                .build();
        return RateLimiter.of("synoptic-users-by-id", config);
    }

    /**
     * Rate limiter for user-by-username endpoint.
     */
    @Bean
    public RateLimiter synopticUserByUsernameRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(CALLS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMinutes(2)) // Wait up to 2 min for permit
                .build();
        return RateLimiter.of("synoptic-user-by-username", config);
    }

    /**
     * Dedicated thread pool for Synoptic batch operations.
     * Size is set to support 40 concurrent calls per endpoint (80 total).
     */
    @Bean
    public ExecutorService synopticBatchExecutor() {
        return Executors.newFixedThreadPool(100);
    }
}
