package com.bark.twitter.usage;

/**
 * Key for the detailed in-memory usage accumulator.
 * Combines API key, endpoint, type (synoptic/cache), and minute bucket.
 */
public record DetailedUsageKey(
        String apiKey,
        String endpoint,
        String type,  // "synoptic" or "cache"
        String minuteBucket
) {
    public static DetailedUsageKey synoptic(String apiKey, String endpoint) {
        return new DetailedUsageKey(apiKey, endpoint, "synoptic", UsageRecord.currentMinuteBucket());
    }

    public static DetailedUsageKey cache(String apiKey, String endpoint) {
        return new DetailedUsageKey(apiKey, endpoint, "cache", UsageRecord.currentMinuteBucket());
    }
}
