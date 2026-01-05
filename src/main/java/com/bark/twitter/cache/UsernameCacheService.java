package com.bark.twitter.cache;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for caching username to userId mappings.
 * Loads full table on startup and maintains in-memory cache with periodic flush to DynamoDB.
 */
@Service
public class UsernameCacheService {

    private final UsernameCacheRepository repository;

    // In-memory cache: username (lowercase) -> userId
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    // Pending writes to be flushed to DynamoDB
    private final ConcurrentHashMap<String, String> pendingWrites = new ConcurrentHashMap<>();

    public UsernameCacheService(UsernameCacheRepository repository) {
        this.repository = repository;
    }

    /**
     * Loads entire table into memory on startup.
     */
    @PostConstruct
    public void loadFromDynamoDB() {
        try {
            Map<String, String> all = repository.loadAll().join();
            cache.putAll(all);
            System.out.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Loaded " + all.size() + " username mappings from DynamoDB");
        } catch (Exception e) {
            System.err.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Failed to load from DynamoDB: " + e.getMessage());
        }
    }

    /**
     * Gets userId for a single username.
     *
     * @param username the username (case-insensitive)
     * @return the userId or null if not cached
     */
    public String getUserId(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return cache.get(username.toLowerCase());
    }

    /**
     * Batch lookup of userIds for multiple usernames.
     *
     * @param usernames list of usernames to look up
     * @return map of username (lowercase) -> userId for found entries
     */
    public Map<String, String> getUserIds(List<String> usernames) {
        Map<String, String> result = new HashMap<>();
        for (String username : usernames) {
            if (username == null || username.isEmpty()) {
                continue;
            }
            String lower = username.toLowerCase();
            String userId = cache.get(lower);
            if (userId != null) {
                result.put(lower, userId);
            }
        }
        return result;
    }

    /**
     * Caches a username to userId mapping.
     * Adds to in-memory cache immediately and queues for DynamoDB flush.
     *
     * @param username the username (case-insensitive)
     * @param userId   the userId
     */
    public void cacheMapping(String username, String userId) {
        if (username == null || username.isEmpty() || userId == null || userId.isEmpty()) {
            return;
        }
        String lower = username.toLowerCase();

        // Only add to pending if this is a new mapping
        if (!cache.containsKey(lower)) {
            cache.put(lower, userId);
            pendingWrites.put(lower, userId);
        }
    }

    /**
     * Batch caches multiple mappings.
     *
     * @param mappings map of username -> userId
     */
    public void cacheMappings(Map<String, String> mappings) {
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            cacheMapping(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Caches username/userId from a Synoptic user response.
     * Synoptic uses screen_name and user_id fields.
     *
     * @param userNode the JSON node containing user data
     */
    public void cacheFromSynopticUser(JsonNode userNode) {
        if (userNode == null || userNode.isNull()) {
            return;
        }
        String username = userNode.path("screen_name").asText("");
        String userId = userNode.path("user_id").asText("");
        if (!username.isEmpty() && !userId.isEmpty()) {
            cacheMapping(username, userId);
        }
    }

    /**
     * Flushes pending writes to DynamoDB.
     * Called every 5 seconds.
     */
    @Scheduled(fixedRate = 5000)
    public void flushToDynamoDB() {
        if (pendingWrites.isEmpty()) {
            return;
        }

        // Copy and clear pending writes atomically
        Map<String, String> toFlush = new HashMap<>();
        var iterator = pendingWrites.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            toFlush.put(entry.getKey(), entry.getValue());
            iterator.remove();
        }

        if (!toFlush.isEmpty()) {
            repository.saveMappings(toFlush)
                    .thenRun(() -> System.out.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Flushed " + toFlush.size() + " mappings to DynamoDB"))
                    .exceptionally(e -> {
                        System.err.println("[" + System.currentTimeMillis() + "][USERNAME_CACHE] Failed to flush: " + e.getMessage());
                        // Re-add failed writes to pending
                        pendingWrites.putAll(toFlush);
                        return null;
                    });
        }
    }

    /**
     * Returns the current size of the in-memory cache.
     */
    public int getCacheSize() {
        return cache.size();
    }
}
