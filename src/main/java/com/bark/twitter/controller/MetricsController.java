package com.bark.twitter.controller;

import com.bark.twitter.service.LatencyTracker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Hidden
public class MetricsController {

    private static final Map<String, String> ENDPOINT_MAPPING = Map.of(
            "/tweet", "/tweet/{id}",
            "/user", "/user/{idOrHandle}",
            "/community", "/community/{id}",
            "/follows", "/follows",
            "/communities", "/communities"
    );

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Latency metrics for all endpoints.
     * Shows cache-miss latencies if available, otherwise falls back to overall request latencies.
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

            // Use cache-miss timer if available, otherwise use request timer
            Timer latencyTimer = (cacheMissTimer != null && cacheMissTimer.count() > 0)
                    ? cacheMissTimer
                    : requestTimer;

            if (latencyTimer != null && latencyTimer.count() > 0) {
                endpointMetrics.put("meanMs", Math.round(latencyTimer.mean(TimeUnit.MILLISECONDS)));
                endpointMetrics.put("maxMs", Math.round(latencyTimer.max(TimeUnit.MILLISECONDS)));
                addPercentiles(endpointMetrics, latencyTimer);
            }

            result.put(springUri, endpointMetrics);
        }

        return result;
    }

    private void addPercentiles(Map<String, Object> metrics, Timer timer) {
        HistogramSnapshot snapshot = timer.takeSnapshot();
        ValueAtPercentile[] percentiles = snapshot.percentileValues();

        // Default to 0 if no percentiles available
        double p50 = 0, p95 = 0, p99 = 0;

        for (ValueAtPercentile vap : percentiles) {
            double percentile = vap.percentile();
            double valueMs = vap.value(TimeUnit.MILLISECONDS);
            if (Math.abs(percentile - 0.5) < 0.01) {
                p50 = valueMs;
            } else if (Math.abs(percentile - 0.95) < 0.01) {
                p95 = valueMs;
            } else if (Math.abs(percentile - 0.99) < 0.01) {
                p99 = valueMs;
            }
        }

        metrics.put("p50Ms", Math.round(p50));
        metrics.put("p95Ms", Math.round(p95));
        metrics.put("p99Ms", Math.round(p99));
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
                endpointMetrics.put("meanMs", Math.round(timer.mean(TimeUnit.MILLISECONDS)));
                endpointMetrics.put("maxMs", Math.round(timer.max(TimeUnit.MILLISECONDS)));
                addPercentiles(endpointMetrics, timer);

                result.put(springUri, endpointMetrics);
            }
        }

        return result;
    }
}
