package com.bark.twitter.usage;

import com.bark.twitter.config.ApiKeyInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Controller for API usage statistics.
 */
@RestController
@RequestMapping("/usage")
@Tag(name = "Usage", description = "API usage statistics")
public class UsageController {

    private final UsageRepository usageRepository;

    public UsageController(UsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    @GetMapping
    @Operation(summary = "Get usage statistics", description = "Returns usage breakdown by endpoint, day, and hour for the authenticated API key")
    public UsageResponse getUsage(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);

        List<UsageRecord> records = usageRepository.queryAllUsage(apiKey).get();

        return aggregateUsage(records);
    }

    @GetMapping("/all")
    @Operation(summary = "Get detailed usage statistics", description = "Returns usage breakdown with cache misses and hits by endpoint and day")
    public DetailedUsageResponse getDetailedUsage(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);

        // Query both regular usage and detailed usage in parallel
        CompletableFuture<List<UsageRecord>> usageFuture = usageRepository.queryAllUsage(apiKey);
        CompletableFuture<List<DetailedUsageRecord>> detailedFuture = usageRepository.queryAllDetailedUsage(apiKey);

        List<UsageRecord> usageRecords = usageFuture.get();
        List<DetailedUsageRecord> detailedRecords = detailedFuture.get();

        return aggregateDetailedUsage(usageRecords, detailedRecords);
    }

    /**
     * Aggregates minute-level records into endpoint -> day -> hour breakdown.
     */
    private UsageResponse aggregateUsage(List<UsageRecord> records) {
        // Structure: endpoint -> day -> hour -> count
        Map<String, Map<String, Map<String, Long>>> aggregated = new HashMap<>();
        long total = 0;

        for (UsageRecord record : records) {
            String endpoint = record.endpoint();
            String minuteBucket = record.minuteBucket(); // Format: 2025-12-17T10:30
            long count = record.count();

            total += count;

            // Parse day and hour from minute bucket
            String day = minuteBucket.substring(0, 10);  // 2025-12-17
            String hour = minuteBucket.substring(11, 13); // 10

            aggregated
                    .computeIfAbsent(endpoint, k -> new TreeMap<>())
                    .computeIfAbsent(day, k -> new TreeMap<>())
                    .merge(hour, count, Long::sum);
        }

        // Convert to response structure
        Map<String, UsageResponse.EndpointUsage> endpoints = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Map<String, Long>>> endpointEntry : aggregated.entrySet()) {
            String endpoint = endpointEntry.getKey();
            Map<String, Map<String, Long>> dayMap = endpointEntry.getValue();

            long endpointTotal = 0;
            Map<String, UsageResponse.DayUsage> days = new LinkedHashMap<>();

            for (Map.Entry<String, Map<String, Long>> dayEntry : dayMap.entrySet()) {
                String day = dayEntry.getKey();
                Map<String, Long> hourMap = dayEntry.getValue();

                long dayTotal = hourMap.values().stream().mapToLong(Long::longValue).sum();
                endpointTotal += dayTotal;

                days.put(day, new UsageResponse.DayUsage(dayTotal, hourMap));
            }

            endpoints.put(endpoint, new UsageResponse.EndpointUsage(endpointTotal, days));
        }

        return new UsageResponse(total, endpoints);
    }

    private static final double INCOME_PER_CALL = 0.00025;
    private static final double COST_PER_TWEET_SYNOPTIC = 0.0001;
    private static final double COST_PER_USER_SYNOPTIC = 0.00012;
    private static final double COST_PER_COMMUNITY_SYNOPTIC = 0.00024;

    /**
     * Aggregates usage and detailed records into endpoint -> day breakdown with miss/hit counts.
     */
    private DetailedUsageResponse aggregateDetailedUsage(List<UsageRecord> usageRecords, List<DetailedUsageRecord> detailedRecords) {
        // Structure: endpoint -> day -> {total, miss, hit}
        Map<String, Map<String, long[]>> aggregated = new HashMap<>();

        // Aggregate total counts from regular usage records
        for (UsageRecord record : usageRecords) {
            String endpoint = record.endpoint();
            String day = record.minuteBucket().substring(0, 10);  // 2025-12-17
            long count = record.count();

            aggregated.computeIfAbsent(endpoint, k -> new TreeMap<>())
                    .computeIfAbsent(day, k -> new long[3])[0] += count;  // [0] = total
        }

        // Aggregate miss and hit counts from detailed records
        for (DetailedUsageRecord record : detailedRecords) {
            String endpoint = record.endpoint();
            String day = record.minuteBucket().substring(0, 10);
            long count = record.count();

            long[] counts = aggregated.computeIfAbsent(endpoint, k -> new TreeMap<>())
                    .computeIfAbsent(day, k -> new long[3]);

            if ("synoptic".equals(record.type())) {
                counts[1] += count;  // [1] = miss
            } else if ("cache".equals(record.type())) {
                counts[2] += count;  // [2] = hit
            }
        }

        // Build response
        long grandTotal = 0;
        long grandMiss = 0;
        long grandHit = 0;
        double grandIncome = 0;
        double grandCost = 0;
        Map<String, DetailedUsageResponse.EndpointUsage> endpoints = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, long[]>> endpointEntry : aggregated.entrySet()) {
            String endpoint = endpointEntry.getKey();
            Map<String, long[]> dayMap = endpointEntry.getValue();

            long endpointTotal = 0;
            long endpointMiss = 0;
            long endpointHit = 0;
            double endpointIncome = 0;
            double endpointCost = 0;
            Map<String, DetailedUsageResponse.DayUsage> days = new LinkedHashMap<>();

            double costPerMiss = switch (endpoint) {
                case "/tweet" -> COST_PER_TWEET_SYNOPTIC;
                case "/community" -> COST_PER_COMMUNITY_SYNOPTIC;
                default -> COST_PER_USER_SYNOPTIC;
            };

            for (Map.Entry<String, long[]> dayEntry : dayMap.entrySet()) {
                String day = dayEntry.getKey();
                long[] counts = dayEntry.getValue();

                long dayTotal = counts[0];
                long dayMiss = counts[1];
                long dayHit = counts[2];
                double dayIncome = dayTotal * INCOME_PER_CALL;
                double dayCost = dayMiss * costPerMiss;
                double dayProfit = dayIncome - dayCost;

                endpointTotal += dayTotal;
                endpointMiss += dayMiss;
                endpointHit += dayHit;
                endpointIncome += dayIncome;
                endpointCost += dayCost;

                days.put(day, new DetailedUsageResponse.DayUsage(
                        dayTotal, dayMiss, dayHit, dayIncome, dayCost, dayProfit));
            }

            grandTotal += endpointTotal;
            grandMiss += endpointMiss;
            grandHit += endpointHit;
            grandIncome += endpointIncome;
            grandCost += endpointCost;

            double endpointProfit = endpointIncome - endpointCost;
            endpoints.put(endpoint, new DetailedUsageResponse.EndpointUsage(
                    endpointTotal, endpointMiss, endpointHit,
                    endpointIncome, endpointCost, endpointProfit, days));
        }

        double grandProfit = grandIncome - grandCost;
        return new DetailedUsageResponse(grandTotal, grandMiss, grandHit,
                grandIncome, grandCost, grandProfit, endpoints);
    }
}
