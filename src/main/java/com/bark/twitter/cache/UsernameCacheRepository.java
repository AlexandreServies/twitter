package com.bark.twitter.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for username to userId cache in DynamoDB.
 * Schema: pk (username lowercase) -> userId
 */
@Repository
public class UsernameCacheRepository {

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public UsernameCacheRepository(
            DynamoDbAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.username-cache-table:twitter-username-cache}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Scans entire table and returns all username -> userId mappings.
     * Used on startup to populate in-memory cache.
     */
    public CompletableFuture<Map<String, String>> loadAll() {
        return scanAllPaginated(null, new HashMap<>());
    }

    private CompletableFuture<Map<String, String>> scanAllPaginated(
            Map<String, AttributeValue> lastKey,
            Map<String, String> accumulated
    ) {
        ScanRequest.Builder requestBuilder = ScanRequest.builder()
                .tableName(tableName);

        if (lastKey != null) {
            requestBuilder.exclusiveStartKey(lastKey);
        }

        return dynamoDbClient.scan(requestBuilder.build())
                .thenCompose(response -> {
                    for (Map<String, AttributeValue> item : response.items()) {
                        String username = item.get("pk").s();
                        AttributeValue userIdAttr = item.get("userId");
                        if (userIdAttr != null && userIdAttr.s() != null) {
                            accumulated.put(username, userIdAttr.s());
                        }
                    }

                    if (response.lastEvaluatedKey() != null && !response.lastEvaluatedKey().isEmpty()) {
                        return scanAllPaginated(response.lastEvaluatedKey(), accumulated);
                    }
                    return CompletableFuture.completedFuture(accumulated);
                })
                .exceptionally(e -> {
                    System.err.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Failed to scan table: " + e.getMessage());
                    return accumulated;
                });
    }

    /**
     * Batch saves mappings to DynamoDB.
     * Uses BatchWriteItem for efficiency.
     */
    public CompletableFuture<Void> saveMappings(Map<String, String> mappings) {
        if (mappings.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<WriteRequest> writeRequests = new ArrayList<>();
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("pk", AttributeValue.builder().s(entry.getKey()).build());
            item.put("userId", AttributeValue.builder().s(entry.getValue()).build());

            writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());
        }

        // DynamoDB BatchWriteItem has a limit of 25 items per request
        return batchWriteInChunks(writeRequests, 0);
    }

    private CompletableFuture<Void> batchWriteInChunks(List<WriteRequest> requests, int startIndex) {
        if (startIndex >= requests.size()) {
            return CompletableFuture.completedFuture(null);
        }

        int endIndex = Math.min(startIndex + 25, requests.size());
        List<WriteRequest> chunk = requests.subList(startIndex, endIndex);

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                .requestItems(Map.of(tableName, chunk))
                .build();

        return dynamoDbClient.batchWriteItem(request)
                .thenCompose(response -> {
                    // Handle unprocessed items by retrying
                    Map<String, List<WriteRequest>> unprocessed = response.unprocessedItems();
                    if (unprocessed != null && !unprocessed.isEmpty() && unprocessed.containsKey(tableName)) {
                        List<WriteRequest> remaining = new ArrayList<>(unprocessed.get(tableName));
                        remaining.addAll(requests.subList(endIndex, requests.size()));
                        return batchWriteInChunks(remaining, 0);
                    }
                    return batchWriteInChunks(requests, endIndex);
                })
                .exceptionally(e -> {
                    System.err.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Failed to batch write: " + e.getMessage());
                    return null;
                });
    }
}
