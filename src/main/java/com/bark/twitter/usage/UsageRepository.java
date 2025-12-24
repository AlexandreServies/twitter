package com.bark.twitter.usage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for persisting and querying usage data in DynamoDB.
 */
@Repository
public class UsageRepository {

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public UsageRepository(
            DynamoDbAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.usage-table:twitter-relay-usage}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Batch updates counts in DynamoDB asynchronously.
     * Uses UpdateItem with ADD to atomically increment counters.
     * This ensures no data loss even with concurrent updates.
     */
    public void batchUpdateCountsAsync(List<UsageRecord> records) {
        for (UsageRecord record : records) {
            updateCountAsync(record);
        }
    }

    /**
     * Atomically increments the count for a usage record.
     */
    private CompletableFuture<Void> updateCountAsync(UsageRecord record) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.builder().s(record.pk()).build());
        key.put("sk", AttributeValue.builder().s(record.sk()).build());

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":inc", AttributeValue.builder().n(String.valueOf(record.count())).build());
        expressionValues.put(":endpoint", AttributeValue.builder().s(record.endpoint()).build());
        expressionValues.put(":bucket", AttributeValue.builder().s(record.minuteBucket()).build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("ADD #count :inc SET #endpoint = :endpoint, #bucket = :bucket")
                .expressionAttributeNames(Map.of(
                        "#count", "count",
                        "#endpoint", "endpoint",
                        "#bucket", "minuteBucket"
                ))
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbClient.updateItem(request)
                .thenAccept(response -> {})
                .exceptionally(e -> {
                    System.err.println("[USAGE] Failed to update DynamoDB: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Queries usage data for a specific API key within a date range.
     * Returns all minute-level records for aggregation.
     */
    public CompletableFuture<List<UsageRecord>> queryUsage(String apiKey, String startDate, String endDate) {
        String pk = apiKey;

        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("pk = :pk AND sk BETWEEN :start AND :end")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(pk).build(),
                        ":start", AttributeValue.builder().s(startDate).build(),
                        ":end", AttributeValue.builder().s(endDate + "~").build()  // ~ is after Z in ASCII
                ))
                .build();

        return dynamoDbClient.query(request)
                .thenApply(response -> {
                    List<UsageRecord> records = new ArrayList<>();
                    for (Map<String, AttributeValue> item : response.items()) {
                        String sk = item.get("sk").s();
                        String[] parts = sk.split("#", 2);
                        String endpoint = parts[0];
                        String minuteBucket = parts.length > 1 ? parts[1] : "";
                        long count = Long.parseLong(item.get("count").n());

                        records.add(new UsageRecord(apiKey, endpoint, minuteBucket, count));
                    }
                    return records;
                });
    }

    /**
     * Queries all usage data for a specific API key (no date filter).
     * Handles pagination to retrieve all records.
     */
    public CompletableFuture<List<UsageRecord>> queryAllUsage(String apiKey) {
        return queryAllUsagePaginated(apiKey, null, new ArrayList<>());
    }

    private CompletableFuture<List<UsageRecord>> queryAllUsagePaginated(
            String apiKey, Map<String, AttributeValue> lastKey, List<UsageRecord> accumulated) {

        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("pk = :pk")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(apiKey).build()
                ));

        if (lastKey != null) {
            requestBuilder.exclusiveStartKey(lastKey);
        }

        return dynamoDbClient.query(requestBuilder.build())
                .thenCompose(response -> {
                    for (Map<String, AttributeValue> item : response.items()) {
                        String sk = item.get("sk").s();
                        String[] parts = sk.split("#", 2);
                        String endpoint = parts[0];
                        String minuteBucket = parts.length > 1 ? parts[1] : "";
                        long count = Long.parseLong(item.get("count").n());

                        accumulated.add(new UsageRecord(apiKey, endpoint, minuteBucket, count));
                    }

                    if (response.lastEvaluatedKey() != null && !response.lastEvaluatedKey().isEmpty()) {
                        return queryAllUsagePaginated(apiKey, response.lastEvaluatedKey(), accumulated);
                    }
                    return CompletableFuture.completedFuture(accumulated);
                });
    }

    /**
     * Batch updates detailed counts in DynamoDB asynchronously.
     */
    public void batchUpdateDetailedCountsAsync(List<DetailedUsageRecord> records) {
        for (DetailedUsageRecord record : records) {
            updateDetailedCountAsync(record);
        }
    }

    /**
     * Atomically increments the count for a detailed usage record.
     */
    private CompletableFuture<Void> updateDetailedCountAsync(DetailedUsageRecord record) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.builder().s(record.pk()).build());
        key.put("sk", AttributeValue.builder().s(record.sk()).build());

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":inc", AttributeValue.builder().n(String.valueOf(record.count())).build());
        expressionValues.put(":endpoint", AttributeValue.builder().s(record.endpoint()).build());
        expressionValues.put(":type", AttributeValue.builder().s(record.type()).build());
        expressionValues.put(":bucket", AttributeValue.builder().s(record.minuteBucket()).build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("ADD #count :inc SET #endpoint = :endpoint, #type = :type, #bucket = :bucket")
                .expressionAttributeNames(Map.of(
                        "#count", "count",
                        "#endpoint", "endpoint",
                        "#type", "type",
                        "#bucket", "minuteBucket"
                ))
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbClient.updateItem(request)
                .thenAccept(response -> {})
                .exceptionally(e -> {
                    System.err.println("[USAGE] Failed to update detailed DynamoDB: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Queries all detailed usage data for a specific API key (no date filter).
     * Handles pagination to retrieve all records.
     */
    public CompletableFuture<List<DetailedUsageRecord>> queryAllDetailedUsage(String apiKey) {
        String pk = "detail#" + apiKey;
        return queryAllDetailedUsagePaginated(apiKey, pk, null, new ArrayList<>());
    }

    private CompletableFuture<List<DetailedUsageRecord>> queryAllDetailedUsagePaginated(
            String apiKey, String pk, Map<String, AttributeValue> lastKey, List<DetailedUsageRecord> accumulated) {

        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("pk = :pk")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s(pk).build()
                ));

        if (lastKey != null) {
            requestBuilder.exclusiveStartKey(lastKey);
        }

        return dynamoDbClient.query(requestBuilder.build())
                .thenCompose(response -> {
                    for (Map<String, AttributeValue> item : response.items()) {
                        String sk = item.get("sk").s();
                        // Format: <endpoint>#<type>#<minute-bucket>
                        String[] parts = sk.split("#", 3);
                        String endpoint = parts[0];
                        String type = parts.length > 1 ? parts[1] : "";
                        String minuteBucket = parts.length > 2 ? parts[2] : "";
                        long count = Long.parseLong(item.get("count").n());

                        accumulated.add(new DetailedUsageRecord(apiKey, endpoint, type, minuteBucket, count));
                    }

                    if (response.lastEvaluatedKey() != null && !response.lastEvaluatedKey().isEmpty()) {
                        return queryAllDetailedUsagePaginated(apiKey, pk, response.lastEvaluatedKey(), accumulated);
                    }
                    return CompletableFuture.completedFuture(accumulated);
                });
    }
}
