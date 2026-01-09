package com.bark.twitter.usage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for tracking detailed API usage with minimal performance impact.
 * Uses in-memory accumulation with periodic async flush to DynamoDB.
 *
 * Tracks three types:
 * - hit: Cache hits (served from local cache)
 * - miss: Cache misses (had to call Synoptic)
 * - found: Items Synoptic actually found and charged for (subset of miss, used for cost calculation)
 */
@Service
public class DetailedUsageTrackingService {

    private final ConcurrentHashMap<DetailedUsageKey, LongAdder> accumulator = new ConcurrentHashMap<>();
    private final UsageRepository repository;

    public DetailedUsageTrackingService(UsageRepository repository) {
        this.repository = repository;
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
        DetailedUsageKey key = DetailedUsageKey.hit(apiKeyHash, endpoint);
        accumulator.computeIfAbsent(key, k -> new LongAdder()).add(count);
    }

    /**
     * Records a cache miss (had to call Synoptic). Zero-latency impact.
     */
    public void recordCacheMiss(String apiKeyHash, String endpoint) {
        recordCacheMisses(apiKeyHash, endpoint, 1);
    }

    /**
     * Records multiple cache misses. Used for batch operations.
     */
    public void recordCacheMisses(String apiKeyHash, String endpoint, long count) {
        if (count <= 0) return;
        DetailedUsageKey key = DetailedUsageKey.miss(apiKeyHash, endpoint);
        accumulator.computeIfAbsent(key, k -> new LongAdder()).add(count);
    }

    /**
     * Records a found item (Synoptic returned data and charged for it). Zero-latency impact.
     */
    public void recordFound(String apiKeyHash, String endpoint) {
        recordFoundItems(apiKeyHash, endpoint, 1);
    }

    /**
     * Records multiple found items. Used for batch operations.
     */
    public void recordFoundItems(String apiKeyHash, String endpoint, long count) {
        if (count <= 0) return;
        DetailedUsageKey key = DetailedUsageKey.found(apiKeyHash, endpoint);
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
