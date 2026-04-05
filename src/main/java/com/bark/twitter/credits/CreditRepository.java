package com.bark.twitter.credits;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
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
     * Atomically claims credits from DynamoDB using a conditional decrement.
     * Tries to claim the requested amount; if not enough, claims whatever is available.
     * Returns the number of credits actually claimed.
     */
    public long claimCredits(String apiKey, long amount) {
        Map<String, AttributeValue> key = Map.of("pk", AttributeValue.builder().s(apiKey).build());

        // First attempt: claim full amount if enough credits exist
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #credits = #credits - :amount")
                    .conditionExpression("#credits >= :amount")
                    .expressionAttributeNames(Map.of("#credits", "credits"))
                    .expressionAttributeValues(Map.of(":amount", AttributeValue.builder().n(String.valueOf(amount)).build()))
                    .build();

            dynamoDbClient.updateItem(request).join();
            return amount;
        } catch (Exception e) {
            if (!isCausedByConditionCheck(e)) {
                System.err.println("[" + System.currentTimeMillis() + "][CREDITS] Failed to claim credits: " + e.getMessage());
                return 0;
            }
        }

        // Fallback: claim whatever remains (set to 0 and get old value)
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #credits = :zero")
                    .conditionExpression("#credits > :zero")
                    .expressionAttributeNames(Map.of("#credits", "credits"))
                    .expressionAttributeValues(Map.of(":zero", AttributeValue.builder().n("0").build()))
                    .returnValues(ReturnValue.ALL_OLD)
                    .build();

            var response = dynamoDbClient.updateItem(request).join();
            AttributeValue oldCredits = response.attributes().get("credits");
            if (oldCredits != null && oldCredits.n() != null) {
                return Long.parseLong(oldCredits.n());
            }
            return 0;
        } catch (Exception e) {
            if (!isCausedByConditionCheck(e)) {
                System.err.println("[" + System.currentTimeMillis() + "][CREDITS] Failed to claim remaining credits: " + e.getMessage());
            }
            return 0;
        }
    }

    /**
     * Atomically adds credits for an API key.
     */
    public CompletableFuture<Void> addCredits(String apiKey, long amount) {
        return updateCredits(apiKey, amount);
    }

    /**
     * Atomically decrements credits for an API key.
     */
    public CompletableFuture<Void> decrementCredits(String apiKey, long amount) {
        return updateCredits(apiKey, -amount);
    }

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

    private boolean isCausedByConditionCheck(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConditionalCheckFailedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
