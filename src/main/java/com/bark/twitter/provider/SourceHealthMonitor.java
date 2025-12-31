package com.bark.twitter.provider;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Monitors health metrics (errors and latency) for each data source per endpoint.
 * Uses rolling 3-minute windows for all calculations.
 * Requires minimum 3 minutes of data before triggering fallback.
 */
@Component
public class SourceHealthMonitor {

    private static final long WINDOW_MS = 180_000; // 3 minute rolling window
    private static final long MIN_DATA_AGE_MS = 180_000; // Need 3 minutes of data before triggering fallback

    // Thresholds for detecting issues (trigger fallback)
    private static final int ERROR_THRESHOLD = 10;
    private static final long SYNOPTIC_LATENCY_ISSUE_THRESHOLD_MS = 800;
    private static final long TWITTERAPI_LATENCY_ISSUE_THRESHOLD_MS = 2000;

    // Thresholds for recovery (switch back to primary)
    private static final long SYNOPTIC_LATENCY_HEALTHY_THRESHOLD_MS = 500;
    private static final long TWITTERAPI_LATENCY_HEALTHY_THRESHOLD_MS = 1000;

    private final Map<SourceEndpointKey, Deque<TimestampedMetric>> latencyMetrics = new ConcurrentHashMap<>();
    private final Map<SourceEndpointKey, Deque<Long>> errorTimestamps = new ConcurrentHashMap<>();
    private final Map<SourceEndpointKey, Long> firstDataTimestamp = new ConcurrentHashMap<>();

    public enum Endpoint {
        TWEET, USER, COMMUNITY
    }

    public enum Source {
        SYNOPTIC, TWITTERAPI
    }

    private record SourceEndpointKey(Source source, Endpoint endpoint) {}

    private record TimestampedMetric(long timestamp, long value) {}

    /**
     * Records a successful request with its latency.
     */
    public void recordLatency(Source source, Endpoint endpoint, long latencyMs) {
        SourceEndpointKey key = new SourceEndpointKey(source, endpoint);
        long now = System.currentTimeMillis();
        firstDataTimestamp.putIfAbsent(key, now);
        Deque<TimestampedMetric> deque = latencyMetrics.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(new TimestampedMetric(now, latencyMs));
        pruneOldMetrics(deque);
    }

    /**
     * Records an error for the given source and endpoint.
     */
    public void recordError(Source source, Endpoint endpoint) {
        SourceEndpointKey key = new SourceEndpointKey(source, endpoint);
        Deque<Long> deque = errorTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(System.currentTimeMillis());
        pruneOldErrors(deque);
    }

    /**
     * Checks if the source is experiencing issues (should trigger fallback).
     * Issues = 10+ errors in last 3 minutes OR median latency above threshold.
     * Requires at least 3 minutes of data before triggering.
     */
    public boolean hasIssues(Source source, Endpoint endpoint) {
        SourceEndpointKey key = new SourceEndpointKey(source, endpoint);

        // Don't trigger fallback until we have enough data (3 minutes)
        Long firstData = firstDataTimestamp.get(key);
        if (firstData == null || System.currentTimeMillis() - firstData < MIN_DATA_AGE_MS) {
            return false; // Not enough data yet
        }

        // Check error count
        int errorCount = getErrorCount(key);
        if (errorCount >= ERROR_THRESHOLD) {
            return true;
        }

        // Check median latency
        long medianLatency = getMedianLatency(key);
        if (medianLatency < 0) {
            return false; // No data yet
        }

        long threshold = source == Source.SYNOPTIC
                ? SYNOPTIC_LATENCY_ISSUE_THRESHOLD_MS
                : TWITTERAPI_LATENCY_ISSUE_THRESHOLD_MS;

        return medianLatency > threshold;
    }

    /**
     * Checks if the source is healthy (can switch back from fallback).
     * Healthy = 0 errors in last minute AND median latency below healthy threshold.
     */
    public boolean isHealthy(Source source, Endpoint endpoint) {
        SourceEndpointKey key = new SourceEndpointKey(source, endpoint);

        // Must have zero errors
        int errorCount = getErrorCount(key);
        if (errorCount > 0) {
            return false;
        }

        // Check median latency
        long medianLatency = getMedianLatency(key);
        if (medianLatency < 0) {
            return false; // No data yet, can't confirm healthy
        }

        long threshold = source == Source.SYNOPTIC
                ? SYNOPTIC_LATENCY_HEALTHY_THRESHOLD_MS
                : TWITTERAPI_LATENCY_HEALTHY_THRESHOLD_MS;

        return medianLatency <= threshold;
    }

    /**
     * Gets current error count in the rolling window.
     */
    public int getErrorCount(Source source, Endpoint endpoint) {
        return getErrorCount(new SourceEndpointKey(source, endpoint));
    }

    /**
     * Gets current median latency in the rolling window, or -1 if no data.
     */
    public long getMedianLatency(Source source, Endpoint endpoint) {
        return getMedianLatency(new SourceEndpointKey(source, endpoint));
    }

    private int getErrorCount(SourceEndpointKey key) {
        Deque<Long> deque = errorTimestamps.get(key);
        if (deque == null) {
            return 0;
        }
        pruneOldErrors(deque);
        return deque.size();
    }

    private long getMedianLatency(SourceEndpointKey key) {
        Deque<TimestampedMetric> deque = latencyMetrics.get(key);
        if (deque == null || deque.isEmpty()) {
            return -1;
        }
        pruneOldMetrics(deque);
        if (deque.isEmpty()) {
            return -1;
        }

        long[] values = deque.stream()
                .mapToLong(TimestampedMetric::value)
                .sorted()
                .toArray();

        int mid = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[mid - 1] + values[mid]) / 2;
        }
        return values[mid];
    }

    private void pruneOldMetrics(Deque<TimestampedMetric> deque) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!deque.isEmpty() && deque.peekFirst().timestamp() < cutoff) {
            deque.pollFirst();
        }
    }

    private void pruneOldErrors(Deque<Long> deque) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
            deque.pollFirst();
        }
    }

    /**
     * Returns a health summary for logging/debugging.
     */
    public String getHealthSummary(Source source, Endpoint endpoint) {
        SourceEndpointKey key = new SourceEndpointKey(source, endpoint);
        int errors = getErrorCount(key);
        long median = getMedianLatency(key);
        boolean hasIssues = hasIssues(source, endpoint);
        boolean isHealthy = isHealthy(source, endpoint);
        return String.format("[%s][%s] errors=%d, medianLatency=%dms, hasIssues=%s, isHealthy=%s",
                source, endpoint, errors, median, hasIssues, isHealthy);
    }
}
