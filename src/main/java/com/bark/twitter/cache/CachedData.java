package com.bark.twitter.cache;

/**
 * Generic wrapper for cached data with billing tracking.
 * Stores the data along with timestamps for cache expiry and billing period tracking.
 *
 * @param data     The cached data
 * @param cachedAt Timestamp when data was fetched (used for cache TTL)
 * @param billedAt Timestamp when last billed (used for billing period)
 */
public record CachedData<T>(
        T data,
        long cachedAt,
        long billedAt
) {
    /**
     * Creates a new CachedData with current timestamp for both cachedAt and billedAt.
     */
    public static <T> CachedData<T> of(T data) {
        long now = System.currentTimeMillis();
        return new CachedData<>(data, now, now);
    }

    /**
     * Checks if this cached entry should be billed.
     *
     * @param billingPeriodMs The billing period in milliseconds. 0 means bill every request.
     * @return true if billing period has expired (or is 0), false otherwise
     */
    public boolean isBillable(long billingPeriodMs) {
        if (billingPeriodMs == 0) {
            return true; // bill every request
        }
        return System.currentTimeMillis() - billedAt > billingPeriodMs;
    }

    /**
     * Returns a new CachedData with updated billedAt timestamp.
     * Used after successfully billing to reset the billing window.
     */
    public CachedData<T> withUpdatedBilling() {
        return new CachedData<>(data, cachedAt, System.currentTimeMillis());
    }
}
