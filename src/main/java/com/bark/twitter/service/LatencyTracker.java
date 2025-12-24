package com.bark.twitter.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Tracks latency for cache-miss requests (requests that hit Synoptic).
 */
@Component
public class LatencyTracker {

    public static final String CACHE_MISS_TIMER = "api.cache.miss";

    private final MeterRegistry meterRegistry;

    public LatencyTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCacheMiss(String endpoint, long durationMs) {
        Timer.builder(CACHE_MISS_TIMER)
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
