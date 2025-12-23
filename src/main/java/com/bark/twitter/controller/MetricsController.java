package com.bark.twitter.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Hidden
public class MetricsController {

    private static final List<String> TRACKED_ENDPOINTS = List.of(
            "/tweet/{id}",
            "/user/{idOrHandle}",
            "/community/{id}"
    );

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new HashMap<>();

        for (String endpoint : TRACKED_ENDPOINTS) {
            Timer timer = meterRegistry.find("http.server.requests")
                    .tag("uri", endpoint)
                    .timer();

            if (timer != null && timer.count() > 0) {
                Map<String, Object> endpointMetrics = new HashMap<>();
                endpointMetrics.put("count", timer.count());
                endpointMetrics.put("meanMs", timer.mean(TimeUnit.MILLISECONDS));
                endpointMetrics.put("maxMs", timer.max(TimeUnit.MILLISECONDS));

                // Percentiles
                double p50 = timer.percentile(0.5, TimeUnit.MILLISECONDS);
                double p95 = timer.percentile(0.95, TimeUnit.MILLISECONDS);
                double p99 = timer.percentile(0.99, TimeUnit.MILLISECONDS);

                endpointMetrics.put("p50Ms", p50);
                endpointMetrics.put("p95Ms", p95);
                endpointMetrics.put("p99Ms", p99);

                result.put(endpoint, endpointMetrics);
            }
        }

        return result;
    }
}
