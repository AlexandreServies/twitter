package com.bark.twitter.usage;

import java.util.Map;

/**
 * Response structure for the /usage endpoint.
 * Provides usage breakdown by endpoint, day, and hour.
 */
public record UsageResponse(
        long totalRequests,
        Map<String, EndpointUsage> endpoints
) {

    public record EndpointUsage(
            long total,
            Map<String, DayUsage> days
    ) {}

    public record DayUsage(
            long total,
            Map<String, Long> hours
    ) {}
}
