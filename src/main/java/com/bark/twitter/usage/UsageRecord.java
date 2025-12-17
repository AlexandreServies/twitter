package com.bark.twitter.usage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Represents a usage record for a specific API key, endpoint, and minute bucket.
 */
public record UsageRecord(
        String apiKey,
        String endpoint,
        String minuteBucket,  // Format: 2025-12-17T10:30
        long count
) {
    private static final DateTimeFormatter MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneOffset.UTC);

    /**
     * Creates a partition key for DynamoDB.
     */
    public String pk() {
        return apiKey;
    }

    /**
     * Creates a sort key for DynamoDB: <endpoint>#<minute-bucket>
     */
    public String sk() {
        return endpoint + "#" + minuteBucket;
    }

    /**
     * Gets the current minute bucket string.
     */
    public static String currentMinuteBucket() {
        return MINUTE_FORMATTER.format(Instant.now());
    }

    /**
     * Gets the minute bucket for a specific instant.
     */
    public static String minuteBucketFor(Instant instant) {
        return MINUTE_FORMATTER.format(instant);
    }
}
