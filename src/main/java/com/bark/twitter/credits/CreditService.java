package com.bark.twitter.credits;

import com.bark.twitter.config.SecurityConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for managing API credits with in-memory tracking.
 * Credits are cached in memory for fast access and decremented atomically.
 * Pending decrements are flushed to DynamoDB periodically by UsageTrackingService.
 */
@Service
public class CreditService {

    private final CreditRepository creditRepository;
    private final SecurityConfig securityConfig;

    // In-memory cache of current credits per API key
    private final ConcurrentHashMap<String, AtomicLong> creditsCache = new ConcurrentHashMap<>();

    // Pending decrements to be flushed to DynamoDB
    private final ConcurrentHashMap<String, LongAdder> pendingDecrements = new ConcurrentHashMap<>();

    public CreditService(CreditRepository creditRepository, SecurityConfig securityConfig) {
        this.creditRepository = creditRepository;
        this.securityConfig = securityConfig;
    }

    /**
     * Loads credits for all configured API keys on startup.
     */
    @PostConstruct
    public void loadCreditsOnStartup() {
        for (String apiKey : securityConfig.getApiKeys()) {
            try {
                long credits = creditRepository.getCredits(apiKey).join();
                creditsCache.put(apiKey, new AtomicLong(credits));
                System.out.println("[" + System.currentTimeMillis() + "][CREDITS] Loaded " + credits + " credits for API key");
            } catch (Exception e) {
                System.err.println("[" + System.currentTimeMillis() + "][CREDITS] Failed to load credits for API key: " + e.getMessage());
                creditsCache.put(apiKey, new AtomicLong(0));
            }
        }
    }

    /**
     * Checks if the API key has credits remaining.
     */
    public boolean hasCredits(String apiKey) {
        AtomicLong credits = creditsCache.get(apiKey);
        return credits != null && credits.get() > 0;
    }

    /**
     * Gets the current credit balance for an API key.
     */
    public long getCredits(String apiKey) {
        AtomicLong credits = creditsCache.get(apiKey);
        return credits != null ? credits.get() : 0;
    }

    /**
     * Decrements one credit from the API key.
     * This is called for each API request.
     * Returns true if credit was successfully decremented, false if no credits available.
     */
    public boolean decrementCredit(String apiKey) {
        AtomicLong credits = creditsCache.get(apiKey);
        if (credits == null) {
            return false;
        }

        // Atomically decrement if positive
        long current;
        do {
            current = credits.get();
            if (current <= 0) {
                return false;
            }
        } while (!credits.compareAndSet(current, current - 1));

        // Track pending decrement for flush
        pendingDecrements.computeIfAbsent(apiKey, k -> new LongAdder()).increment();
        return true;
    }

    /**
     * Adds credits to an API key.
     * Updates both in-memory cache and DynamoDB immediately.
     */
    public void addCredits(String apiKey, long amount) {
        // Update in-memory cache
        creditsCache.computeIfAbsent(apiKey, k -> new AtomicLong(0)).addAndGet(amount);

        // Update DynamoDB immediately
        creditRepository.addCredits(apiKey, amount);

        System.out.println("[" + System.currentTimeMillis() + "][CREDITS] Added " + amount + " credits to API key");
    }

    /**
     * Flushes pending decrements to DynamoDB.
     * Called by UsageTrackingService during its periodic flush.
     */
    public void flushDecrements() {
        if (pendingDecrements.isEmpty()) {
            return;
        }

        var iterator = pendingDecrements.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LongAdder> entry = iterator.next();
            String apiKey = entry.getKey();
            long decrementAmount = entry.getValue().sumThenReset();

            if (decrementAmount > 0) {
                creditRepository.decrementCredits(apiKey, decrementAmount);
            }
        }
    }
}
