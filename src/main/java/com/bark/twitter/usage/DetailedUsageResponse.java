package com.bark.twitter.usage;

import java.util.Map;

/**
 * Response structure for the /usage-all endpoint.
 * Provides usage breakdown with synoptic calls and cache hits by endpoint and day.
 */
public record DetailedUsageResponse(
        long total,
        long synoptic,
        long cache,
        double income,
        double cost,
        double profit,
        Map<String, EndpointUsage> endpoints
) {

    public record EndpointUsage(
            long total,
            long synoptic,
            long cache,
            double income,
            double cost,
            double profit,
            Map<String, DayUsage> days
    ) {}

    public record DayUsage(
            long total,
            long synoptic,
            long cache,
            double income,
            double cost,
            double profit
    ) {}
}
