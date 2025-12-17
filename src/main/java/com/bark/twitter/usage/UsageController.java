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

    /**
     * Aggregates minute-level records into endpoint -> day -> hour breakdown.
     */
    private UsageResponse aggregateUsage(List<UsageRecord> records) {
        // Structure: endpoint -> day -> hour -> count
        Map<String, Map<String, Map<String, Long>>> aggregated = new HashMap<>();
        long totalRequests = 0;

        for (UsageRecord record : records) {
            String endpoint = record.endpoint();
            String minuteBucket = record.minuteBucket(); // Format: 2025-12-17T10:30
            long count = record.count();

            totalRequests += count;

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

        return new UsageResponse(totalRequests, endpoints);
    }
}
