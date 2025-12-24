package com.bark.twitter.controller;

import com.bark.twitter.service.LatencyTracker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Hidden
public class MetricsController {

    private static final Map<String, String> ENDPOINT_MAPPING = Map.of(
            "/tweet", "/tweet/{id}",
            "/user", "/user/{idOrHandle}",
            "/community", "/community/{id}"
    );

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Cache-miss latencies with total request counts.
     * Latency metrics exclude cache hits to show true Synoptic call performance.
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : ENDPOINT_MAPPING.entrySet()) {
            String endpoint = entry.getKey();
            String springUri = entry.getValue();

            Timer cacheMissTimer = meterRegistry.find(LatencyTracker.CACHE_MISS_TIMER)
                    .tag("endpoint", endpoint)
                    .timer();

            Timer requestTimer = meterRegistry.find("http.server.requests")
                    .tag("uri", springUri)
                    .timer();

            long totalCount = requestTimer != null ? requestTimer.count() : 0;
            if (totalCount == 0) {
                continue;
            }

            Map<String, Object> endpointMetrics = new LinkedHashMap<>();
            endpointMetrics.put("count", totalCount);

            if (cacheMissTimer != null && cacheMissTimer.count() > 0) {
                endpointMetrics.put("meanMs", cacheMissTimer.mean(TimeUnit.MILLISECONDS));
                endpointMetrics.put("maxMs", cacheMissTimer.max(TimeUnit.MILLISECONDS));
                endpointMetrics.put("p50Ms", cacheMissTimer.percentile(0.5, TimeUnit.MILLISECONDS));
                endpointMetrics.put("p95Ms", cacheMissTimer.percentile(0.95, TimeUnit.MILLISECONDS));
                endpointMetrics.put("p99Ms", cacheMissTimer.percentile(0.99, TimeUnit.MILLISECONDS));
            }

            result.put(springUri, endpointMetrics);
        }

        return result;
    }

    /**
     * Overall latencies including cache hits.
     */
    @GetMapping("/metrics/all")
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String springUri : ENDPOINT_MAPPING.values()) {
            Timer timer = meterRegistry.find("http.server.requests")
                    .tag("uri", springUri)
                    .timer();

            if (timer != null && timer.count() > 0) {
                Map<String, Object> endpointMetrics = new LinkedHashMap<>();
                endpointMetrics.put("count", timer.count());
                endpointMetrics.put("meanMs", timer.mean(TimeUnit.MILLISECONDS));
                endpointMetrics.put("maxMs", timer.max(TimeUnit.MILLISECONDS));
                endpointMetrics.put("p50Ms", timer.percentile(0.5, TimeUnit.MILLISECONDS));
                endpointMetrics.put("p95Ms", timer.percentile(0.95, TimeUnit.MILLISECONDS));
                endpointMetrics.put("p99Ms", timer.percentile(0.99, TimeUnit.MILLISECONDS));

                result.put(springUri, endpointMetrics);
            }
        }

        return result;
    }
}
