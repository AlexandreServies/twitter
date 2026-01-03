package com.bark.twitter.credits;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for managing API credits in DynamoDB.
 */
@Repository
public class CreditRepository {

    private final DynamoDbAsyncClient dynamoDbClient;
    private final String tableName;

    public CreditRepository(
            DynamoDbAsyncClient dynamoDbClient,
            @Value("${aws.dynamodb.credits-table:twitter-relay-credits}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Fetches current credits for an API key.
     * Returns 0 if the key doesn't exist in the table.
     */
    public CompletableFuture<Long> getCredits(String apiKey) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.builder().s(apiKey).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        return dynamoDbClient.getItem(request)
                .thenApply(response -> {
                    if (response.item() == null || response.item().isEmpty()) {
                        return 0L;
                    }
                    AttributeValue creditsAttr = response.item().get("credits");
                    if (creditsAttr == null || creditsAttr.n() == null) {
                        return 0L;
                    }
                    return Long.parseLong(creditsAttr.n());
                })
                .exceptionally(e -> {
                    System.err.println("[" + System.currentTimeMillis() + "][CREDITS] Failed to get credits: " + e.getMessage());
                    return 0L;
                });
    }

    /**
     * Atomically decrements credits for an API key.
     * Uses ADD with negative value for atomic decrement.
     */
    public CompletableFuture<Void> decrementCredits(String apiKey, long amount) {
        return updateCredits(apiKey, -amount);
    }

    /**
     * Atomically adds credits for an API key.
     */
    public CompletableFuture<Void> addCredits(String apiKey, long amount) {
        return updateCredits(apiKey, amount);
    }

    /**
     * Atomically updates credits by the given delta (positive or negative).
     */
    private CompletableFuture<Void> updateCredits(String apiKey, long delta) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.builder().s(apiKey).build());

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":delta", AttributeValue.builder().n(String.valueOf(delta)).build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("ADD #credits :delta")
                .expressionAttributeNames(Map.of("#credits", "credits"))
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbClient.updateItem(request)
                .thenAccept(response -> {})
                .exceptionally(e -> {
                    System.err.println("[" + System.currentTimeMillis() + "][CREDITS] Failed to update credits: " + e.getMessage());
                    return null;
                });
    }
}
