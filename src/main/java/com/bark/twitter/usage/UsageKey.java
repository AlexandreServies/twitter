package com.bark.twitter.usage;

/**
 * Key for the in-memory usage accumulator.
 * Combines API key, endpoint, and minute bucket.
 */
public record UsageKey(
        String apiKey,
        String endpoint,
        String minuteBucket
) {
    public static UsageKey of(String apiKey, String endpoint) {
        return new UsageKey(apiKey, endpoint, UsageRecord.currentMinuteBucket());
    }
}
