package com.bark.twitter.usage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for tracking detailed API usage (synoptic vs cache) with minimal performance impact.
 * Uses in-memory accumulation with periodic async flush to DynamoDB.
 */
@Service
public class DetailedUsageTrackingService {

    private final ConcurrentHashMap<DetailedUsageKey, LongAdder> accumulator = new ConcurrentHashMap<>();
    private final UsageRepository repository;

    public DetailedUsageTrackingService(UsageRepository repository) {
        this.repository = repository;
    }

    /**
     * Records an API call (cache miss, fetched from upstream provider). Zero-latency impact.
     */
    public void recordApiCall(String apiKeyHash, String endpoint) {
        recordApiCalls(apiKeyHash, endpoint, 1);
    }

    /**
     * Records multiple API calls. Used for batch operations.
     */
    public void recordApiCalls(String apiKeyHash, String endpoint, long count) {
        if (count <= 0) return;
        DetailedUsageKey key = DetailedUsageKey.synoptic(apiKeyHash, endpoint);
        accumulator.computeIfAbsent(key, k -> new LongAdder()).add(count);
    }

    /**
     * Records a cache hit. Zero-latency impact.
     */
    public void recordCacheHit(String apiKeyHash, String endpoint) {
        recordCacheHits(apiKeyHash, endpoint, 1);
    }

    /**
     * Records multiple cache hits. Used for batch operations.
     */
    public void recordCacheHits(String apiKeyHash, String endpoint, long count) {
        if (count <= 0) return;
        DetailedUsageKey key = DetailedUsageKey.cache(apiKeyHash, endpoint);
        accumulator.computeIfAbsent(key, k -> new LongAdder()).add(count);
    }

    /**
     * Flushes accumulated counts to DynamoDB every 5 seconds.
     * Uses swap-and-clear pattern to minimize lock contention.
     */
    @Scheduled(fixedRate = 5000)
    public void flushToDynamoDB() {
        if (accumulator.isEmpty()) {
            return;
        }

        List<DetailedUsageRecord> records = new ArrayList<>();

        var iterator = accumulator.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DetailedUsageKey, LongAdder> entry = iterator.next();
            DetailedUsageKey key = entry.getKey();
            long count = entry.getValue().sumThenReset();

            if (count > 0) {
                records.add(new DetailedUsageRecord(
                        key.apiKey(),
                        key.endpoint(),
                        key.type(),
                        key.minuteBucket(),
                        count
                ));
            }

            // Remove old minute buckets to prevent memory leak
            String currentBucket = UsageRecord.currentMinuteBucket();
            if (!key.minuteBucket().equals(currentBucket)) {
                iterator.remove();
            }
        }

        if (!records.isEmpty()) {
            repository.batchUpdateDetailedCountsAsync(records);
        }
    }
}
