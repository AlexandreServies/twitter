package com.bark.twitter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks latency for cache-miss requests (requests that hit the data provider).
 */
@Component
public class LatencyTracker {

    public static final String CACHE_MISS_TIMER = "api.cache.miss";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    public LatencyTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCacheMiss(String endpoint, long durationMs) {
        Timer timer = timers.computeIfAbsent(endpoint, e ->
                Timer.builder(CACHE_MISS_TIMER)
                        .tag("endpoint", e)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .maximumExpectedValue(Duration.ofSeconds(30))
                        .register(meterRegistry)
        );
        timer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
