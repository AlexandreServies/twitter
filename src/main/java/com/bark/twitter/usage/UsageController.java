package com.bark.twitter.usage;

import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.credits.CreditService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final CreditService creditService;

    public UsageController(UsageRepository usageRepository, CreditService creditService) {
        this.usageRepository = usageRepository;
        this.creditService = creditService;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping
    @Operation(summary = "Get usage statistics", description = "Returns usage breakdown by endpoint, day, and hour for the authenticated API key. Defaults to last 30 days if no date range specified.")
    public UsageResponse getUsage(
            HttpServletRequest request,
            @Parameter(description = "Start date (inclusive) in yyyy-MM-dd format. Defaults to 30 days ago.")
            @RequestParam(required = false) String from,
            @Parameter(description = "End date (inclusive) in yyyy-MM-dd format. Defaults to today.")
            @RequestParam(required = false) String to
    ) throws ExecutionException, InterruptedException {
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);

        // Default to last 30 days if not specified
        LocalDate endDate = (to != null) ? LocalDate.parse(to, DATE_FORMAT) : LocalDate.now();
        LocalDate startDate = (from != null) ? LocalDate.parse(from, DATE_FORMAT) : endDate.minusDays(29);

        String startStr = startDate.format(DATE_FORMAT);
        String endStr = endDate.format(DATE_FORMAT);

        List<UsageRecord> records = usageRepository.queryUsage(apiKey, startStr, endStr).get();
        long creditsRemaining = creditService.getCredits(apiKey);

        return aggregateUsage(records, creditsRemaining);
    }

    @Hidden
    @GetMapping("/all")
    public DetailedUsageResponse getDetailedUsage(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);

        // Query both regular usage and detailed usage in parallel
        CompletableFuture<List<UsageRecord>> usageFuture = usageRepository.queryAllUsage(apiKey);
        CompletableFuture<List<DetailedUsageRecord>> detailedFuture = usageRepository.queryAllDetailedUsage(apiKey);

        List<UsageRecord> usageRecords = usageFuture.get();
        List<DetailedUsageRecord> detailedRecords = detailedFuture.get();
        long creditsRemaining = creditService.getCredits(apiKey);

        return aggregateDetailedUsage(usageRecords, detailedRecords, creditsRemaining);
    }

    /**
     * Aggregates minute-level records into endpoint -> day -> hour breakdown.
     */
    private UsageResponse aggregateUsage(List<UsageRecord> records, long creditsRemaining) {
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

        return new UsageResponse(total, creditsRemaining, endpoints);
    }

    private static final double INCOME_PER_CALL = 0.00025;
    private static final double COST_PER_TWEET_SYNOPTIC = 0.0001;
    private static final double COST_PER_USER_SYNOPTIC = 0.00012;
    private static final double COST_PER_COMMUNITY_SYNOPTIC = 0.00024;
    private static final double COST_PER_COMMUNITY_MEMBER_COUNT_SYNOPTIC = 0.00012;

    /**
     * Aggregates usage and detailed records into endpoint -> day breakdown.
     *
     * Fields calculated:
     * - total: All API requests (hit + miss)
     * - hit: Cache hits (DetailedUsageRecord type="hit" or legacy "cache")
     * - miss: Cache misses (DetailedUsageRecord type="miss")
     * - found: Items Synoptic found and charged for (DetailedUsageRecord type="found" or legacy "synoptic")
     * - billable: Billed requests (from UsageRecord)
     * - income: billable × INCOME_PER_CALL
     * - cost: found × costPerFound
     * - profit: income - cost
     *
     * Note: Legacy data has "synoptic" (now "found") but no "miss" tracking.
     * For legacy data, miss will be 0 and found represents what was tracked.
     * Going forward, miss >= found (miss includes not-found items).
     */
    private DetailedUsageResponse aggregateDetailedUsage(List<UsageRecord> usageRecords, List<DetailedUsageRecord> detailedRecords, long creditsRemaining) {
        // Structure: endpoint -> day -> {billable, hit, miss, found}
        // [0] = billable (from UsageRecord), [1] = hit, [2] = miss, [3] = found
        Map<String, Map<String, long[]>> aggregated = new HashMap<>();

        // Aggregate billable counts from regular usage records
        for (UsageRecord record : usageRecords) {
            String endpoint = record.endpoint();
            String day = record.minuteBucket().substring(0, 10);  // 2025-12-17
            long count = record.count();

            aggregated.computeIfAbsent(endpoint, k -> new TreeMap<>())
                    .computeIfAbsent(day, k -> new long[4])[0] += count;  // [0] = billable
        }

        // Aggregate hit, miss, and found counts from detailed records
        for (DetailedUsageRecord record : detailedRecords) {
            String endpoint = record.endpoint();
            String day = record.minuteBucket().substring(0, 10);
            long count = record.count();

            // Normalize legacy types: "cache" -> "hit", "synoptic" -> "found"
            String type = DetailedUsageKey.normalizeType(record.type());

            long[] counts = aggregated.computeIfAbsent(endpoint, k -> new TreeMap<>())
                    .computeIfAbsent(day, k -> new long[4]);

            switch (type) {
                case "hit" -> counts[1] += count;    // [1] = hit
                case "miss" -> counts[2] += count;   // [2] = miss
                case "found" -> counts[3] += count;  // [3] = found
            }
        }

        // Build response
        long grandTotal = 0;
        long grandHit = 0;
        long grandMiss = 0;
        long grandFound = 0;
        long grandBillable = 0;
        double grandIncome = 0;
        double grandCost = 0;
        Map<String, DetailedUsageResponse.EndpointUsage> endpoints = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, long[]>> endpointEntry : aggregated.entrySet()) {
            String endpoint = endpointEntry.getKey();
            Map<String, long[]> dayMap = endpointEntry.getValue();

            long endpointTotal = 0;
            long endpointHit = 0;
            long endpointMiss = 0;
            long endpointFound = 0;
            long endpointBillable = 0;
            double endpointIncome = 0;
            double endpointCost = 0;
            Map<String, DetailedUsageResponse.DayUsage> days = new LinkedHashMap<>();

            double costPerFound = switch (endpoint) {
                case "/tweet" -> COST_PER_TWEET_SYNOPTIC;
                case "/community" -> COST_PER_COMMUNITY_SYNOPTIC;
                case "/communities" -> COST_PER_COMMUNITY_MEMBER_COUNT_SYNOPTIC;
                default -> COST_PER_USER_SYNOPTIC;
            };

            for (Map.Entry<String, long[]> dayEntry : dayMap.entrySet()) {
                String day = dayEntry.getKey();
                long[] counts = dayEntry.getValue();

                long dayBillable = counts[0];
                long dayHit = counts[1];
                long dayMiss = counts[2];
                long dayFound = counts[3];

                // For legacy data without miss tracking, estimate total from hit + found
                // For new data, total = hit + miss
                long dayTotal = dayMiss > 0 ? (dayHit + dayMiss) : (dayHit + dayFound);

                double dayIncome = dayBillable * INCOME_PER_CALL;  // income from billable requests
                double dayCost = dayFound * costPerFound;  // cost from found items only
                double dayProfit = dayIncome - dayCost;

                endpointTotal += dayTotal;
                endpointHit += dayHit;
                endpointMiss += dayMiss;
                endpointFound += dayFound;
                endpointBillable += dayBillable;
                endpointIncome += dayIncome;
                endpointCost += dayCost;

                days.put(day, new DetailedUsageResponse.DayUsage(
                        dayTotal, dayHit, dayMiss, dayFound, dayBillable, dayIncome, dayCost, dayProfit));
            }

            grandTotal += endpointTotal;
            grandHit += endpointHit;
            grandMiss += endpointMiss;
            grandFound += endpointFound;
            grandBillable += endpointBillable;
            grandIncome += endpointIncome;
            grandCost += endpointCost;

            double endpointProfit = endpointIncome - endpointCost;
            endpoints.put(endpoint, new DetailedUsageResponse.EndpointUsage(
                    endpointTotal, endpointHit, endpointMiss, endpointFound, endpointBillable,
                    endpointIncome, endpointCost, endpointProfit, days));
        }

        double grandProfit = grandIncome - grandCost;
        return new DetailedUsageResponse(grandTotal, grandHit, grandMiss, grandFound, grandBillable,
                grandIncome, grandCost, grandProfit, creditsRemaining, endpoints);
    }
}
