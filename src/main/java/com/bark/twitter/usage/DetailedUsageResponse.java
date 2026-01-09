package com.bark.twitter.usage;

import java.util.Map;

/**
 * Response structure for the /usage/all endpoint.
 * Provides usage breakdown with cache hits, misses, and found items by endpoint and day.
 *
 * Fields:
 * - total: All API requests (hit + miss)
 * - hit: Cache hits (served from local cache)
 * - miss: Cache misses (had to call Synoptic)
 * - found: Items Synoptic actually found and charged for (subset of miss - your cost)
 * - billable: Billed requests (your income source)
 * - income: billable × $0.00025
 * - cost: found × costPerFound (varies by endpoint)
 * - profit: income - cost
 */
public record DetailedUsageResponse(
        long total,
        long hit,
        long miss,
        long found,
        long billable,
        double income,
        double cost,
        double profit,
        long creditsRemaining,
        Map<String, EndpointUsage> endpoints
) {

    public record EndpointUsage(
            long total,
            long hit,
            long miss,
            long found,
            long billable,
            double income,
            double cost,
            double profit,
            Map<String, DayUsage> days
    ) {}

    public record DayUsage(
            long total,
            long hit,
            long miss,
            long found,
            long billable,
            double income,
            double cost,
            double profit
    ) {}
}
