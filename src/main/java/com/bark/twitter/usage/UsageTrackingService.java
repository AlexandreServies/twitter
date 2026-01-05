package com.bark.twitter.usage;

import com.bark.twitter.credits.CreditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for tracking API usage with minimal performance impact.
 * Uses in-memory accumulation with periodic async flush to DynamoDB.
 */
@Service
public class UsageTrackingService {

    private final ConcurrentHashMap<UsageKey, LongAdder> accumulator = new ConcurrentHashMap<>();
    private final UsageRepository repository;
    private final CreditService creditService;

    public UsageTrackingService(UsageRepository repository, CreditService creditService) {
        this.repository = repository;
        this.creditService = creditService;
    }

    /**
     * Records a single API call. This method is designed for zero-latency impact:
     * - ConcurrentHashMap.computeIfAbsent is lock-free for existing keys
     * - LongAdder.increment() is highly concurrent and non-blocking
     */
    public void recordCall(String apiKeyHash, String endpoint) {
        recordCalls(apiKeyHash, endpoint, 1);
    }

    /**
     * Records multiple API calls at once. Used for batch operations.
     */
    public void recordCalls(String apiKeyHash, String endpoint, long count) {
        if (count <= 0) {
            return;
        }
        UsageKey key = UsageKey.of(apiKeyHash, endpoint);
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

        // Swap out the current accumulator entries atomically
        List<UsageRecord> records = new ArrayList<>();

        // Iterate and remove entries that we're flushing
        var iterator = accumulator.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UsageKey, LongAdder> entry = iterator.next();
            UsageKey key = entry.getKey();
            long count = entry.getValue().sumThenReset();

            if (count > 0) {
                records.add(new UsageRecord(
                        key.apiKey(),
                        key.endpoint(),
                        key.minuteBucket(),
                        count
                ));
            }

            // Remove old minute buckets to prevent memory leak
            // Keep current minute bucket for continued accumulation
            String currentBucket = UsageRecord.currentMinuteBucket();
            if (!key.minuteBucket().equals(currentBucket)) {
                iterator.remove();
            }
        }

        if (!records.isEmpty()) {
            // Async write to DynamoDB - fire and forget
            repository.batchUpdateCountsAsync(records);
        }

        // Flush pending credit decrements
        creditService.flushDecrements();
    }
}
