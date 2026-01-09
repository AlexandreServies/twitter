package com.bark.twitter.usage;

/**
 * Key for the detailed in-memory usage accumulator.
 * Combines API key, endpoint, type (hit/miss/found), and minute bucket.
 *
 * Types:
 * - "hit": Cache hits (served from local cache)
 * - "miss": Cache misses (had to call Synoptic)
 * - "found": Items Synoptic actually found and charged for (subset of miss)
 */
public record DetailedUsageKey(
        String apiKey,
        String endpoint,
        String type,  // "hit", "miss", or "found"
        String minuteBucket
) {
    public static DetailedUsageKey hit(String apiKey, String endpoint) {
        return new DetailedUsageKey(apiKey, endpoint, "hit", UsageRecord.currentMinuteBucket());
    }

    public static DetailedUsageKey miss(String apiKey, String endpoint) {
        return new DetailedUsageKey(apiKey, endpoint, "miss", UsageRecord.currentMinuteBucket());
    }

    public static DetailedUsageKey found(String apiKey, String endpoint) {
        return new DetailedUsageKey(apiKey, endpoint, "found", UsageRecord.currentMinuteBucket());
    }

    // Legacy support for reading old "synoptic" records as "found" and "cache" as "hit"
    public static String normalizeType(String type) {
        return switch (type) {
            case "synoptic" -> "found";
            case "cache" -> "hit";
            default -> type;
        };
    }
}
