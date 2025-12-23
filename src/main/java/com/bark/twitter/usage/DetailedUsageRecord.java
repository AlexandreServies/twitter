package com.bark.twitter.usage;

/**
 * Represents a detailed usage record for a specific API key, endpoint, type, and minute bucket.
 */
public record DetailedUsageRecord(
        String apiKey,
        String endpoint,
        String type,       // "synoptic" or "cache"
        String minuteBucket,
        long count
) {
    /**
     * Creates a partition key for DynamoDB.
     * Uses "detail#" prefix to differentiate from regular usage records.
     */
    public String pk() {
        return "detail#" + apiKey;
    }

    /**
     * Creates a sort key for DynamoDB: <endpoint>#<type>#<minute-bucket>
     */
    public String sk() {
        return endpoint + "#" + type + "#" + minuteBucket;
    }
}
