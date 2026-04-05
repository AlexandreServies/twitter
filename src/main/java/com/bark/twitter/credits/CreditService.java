package com.bark.twitter.credits;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing API credits using a batch leasing model.
 * <p>
 * Each instance claims small batches of credits atomically from DynamoDB.
 * API calls decrement from the local batch (zero latency).
 * When a batch is exhausted, a new one is claimed from DynamoDB.
 * A periodic sync returns local batches to DynamoDB every 5 seconds,
 * ensuring all instances converge quickly after add/remove operations.
 * This is multi-instance safe: DynamoDB is always the source of truth.
 */
@Service
public class CreditService {

    private static final long BATCH_SIZE = 500;

    private final CreditRepository creditRepository;

    // Local batch of claimed credits per API key
    private final ConcurrentHashMap<String, AtomicLong> localBatch = new ConcurrentHashMap<>();

    // Per-key locks for batch claiming and returning (prevents concurrent claim/return races)
    private final ConcurrentHashMap<String, Object> claimLocks = new ConcurrentHashMap<>();

    public CreditService(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    /**
     * Gets the current credit balance accurately.
     * Returns this instance's local batch to DynamoDB first, then reads DynamoDB.
     * Used for admin/reporting endpoints only, not for hot-path credit checks.
     */
    public long getCredits(String apiKey) {
        returnLocalBatch(apiKey);
        return creditRepository.getCredits(apiKey).join();
    }

    /**
     * Decrements one credit from the API key.
     * Returns true if credit was successfully decremented, false if no credits available.
     */
    public boolean decrementCredit(String apiKey) {
        return decrementCredits(apiKey, 1);
    }

    /**
     * Decrements multiple credits from the API key.
     * Fast path: uses local batch (no I/O). Slow path: claims a new batch from DynamoDB.
     */
    public boolean decrementCredits(String apiKey, long amount) {
        if (amount <= 0) {
            return true;
        }

        AtomicLong local = localBatch.computeIfAbsent(apiKey, k -> new AtomicLong(0));

        // Fast path: try CAS decrement from local batch
        while (true) {
            long current = local.get();
            if (current >= amount) {
                if (local.compareAndSet(current, current - amount)) {
                    return true;
                }
                continue; // CAS contention, retry
            }

            // Not enough locally — claim more from DynamoDB
            if (!ensureLocalCredits(apiKey, amount)) {
                return false;
            }
            // Retry the CAS with the newly claimed credits
        }
    }

    /**
     * Adds credits to an API key. Updates DynamoDB immediately.
     */
    public void addCredits(String apiKey, long amount) {
        creditRepository.addCredits(apiKey, amount).join();
        System.out.println("[" + System.currentTimeMillis() + "][CREDITS] Added " + amount + " credits for API key " + apiKey);
    }

    /**
     * Removes credits from an API key. Updates DynamoDB immediately.
     * Returns local batch to DynamoDB first to ensure accurate removal.
     */
    public void removeCredits(String apiKey, long amount) {
        returnLocalBatch(apiKey);
        creditRepository.decrementCredits(apiKey, amount).join();
        System.out.println("[" + System.currentTimeMillis() + "][CREDITS] Removed " + amount + " credits for API key " + apiKey);
    }

    /**
     * Periodically returns all local batches to DynamoDB.
     * Ensures all instances converge on the DB truth within 5 seconds,
     * so add/remove operations propagate to all instances quickly.
     */
    @Scheduled(fixedRate = 5000)
    public void syncBatches() {
        for (String apiKey : localBatch.keySet()) {
            returnLocalBatch(apiKey);
        }
    }

    /**
     * Returns any locally held batch credits back to DynamoDB.
     */
    private void returnLocalBatch(String apiKey) {
        AtomicLong local = localBatch.get(apiKey);
        if (local == null) return;

        Object lock = claimLocks.computeIfAbsent(apiKey, k -> new Object());
        synchronized (lock) {
            long remaining = local.getAndSet(0);
            if (remaining > 0) {
                creditRepository.addCredits(apiKey, remaining).join();
            }
        }
    }

    private boolean ensureLocalCredits(String apiKey, long needed) {
        Object lock = claimLocks.computeIfAbsent(apiKey, k -> new Object());
        synchronized (lock) {
            AtomicLong local = localBatch.computeIfAbsent(apiKey, k -> new AtomicLong(0));
            if (local.get() >= needed) {
                return true; // Another thread already claimed
            }

            long toClaim = Math.max(BATCH_SIZE, needed);
            long claimed = creditRepository.claimCredits(apiKey, toClaim);
            if (claimed <= 0) {
                return false;
            }

            local.addAndGet(claimed);
            System.out.println("[" + System.currentTimeMillis() + "][CREDITS] Claimed batch of " + claimed + " credits for API key " + apiKey);
            return local.get() >= needed;
        }
    }
}
