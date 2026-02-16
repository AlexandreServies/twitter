package com.bark.twitter.cache;

import com.bark.twitter.exception.CoalescingTimeoutException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Coalesces concurrent requests for the same key into a single execution.
 *
 * When multiple threads request the same key simultaneously, only the first
 * thread executes the supplier. All other threads wait on the same CompletableFuture
 * and receive the result when it completes.
 *
 * This prevents cache stampedes where N concurrent cache misses for the same key
 * would trigger N external API calls instead of just one.
 */
public class RequestCoalescer<T> {

    private static final long JOINER_TIMEOUT_SECONDS = 5;

    private final ConcurrentHashMap<String, CompletableFuture<T>> inFlight = new ConcurrentHashMap<>();

    /**
     * Execute the supplier for the given key, coalescing concurrent requests.
     *
     * @param key      The cache key
     * @param supplier The supplier to execute (only called by the first requester)
     * @return A CoalescedResult containing the data and whether this was the initiating request
     */
    public CoalescedResult<T> execute(String key, Supplier<T> supplier) {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        CompletableFuture<T> existingFuture = inFlight.putIfAbsent(key, newFuture);

        if (existingFuture == null) {
            // We're the initiator - execute the supplier
            try {
                T result = supplier.get();
                newFuture.complete(result);
                return new CoalescedResult<>(result, true);
            } catch (Exception e) {
                newFuture.completeExceptionally(e);
                throw e;
            } finally {
                inFlight.remove(key, newFuture);
            }
        } else {
            // We're a joiner - wait for the initiator with timeout
            try {
                T result = existingFuture.get(JOINER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return new CoalescedResult<>(result, false);
            } catch (TimeoutException e) {
                // Initiator is taking too long - give up on coalescing, let this request proceed independently
                // This prevents thread starvation if the initiator is stuck
                throw new CoalescingTimeoutException("Coalesced request timed out waiting for initiator", e);
            } catch (CompletionException e) {
                // Unwrap and rethrow the original exception
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            } catch (Exception e) {
                // InterruptedException or ExecutionException
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause != null ? cause : e);
            }
        }
    }

    /**
     * Result of a coalesced request.
     *
     * @param data        The fetched data
     * @param isInitiator True if this was the request that triggered the fetch,
     *                    false if this request joined an existing in-flight request
     */
    public record CoalescedResult<T>(T data, boolean isInitiator) {}
}
