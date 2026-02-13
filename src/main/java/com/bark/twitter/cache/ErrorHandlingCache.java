package com.bark.twitter.cache;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache wrapper that catches and logs exceptions instead of propagating them.
 * Ensures the application continues to function even if the cache (Redis) is down.
 */
public class ErrorHandlingCache implements Cache {

    private final Cache delegate;

    public ErrorHandlingCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        try {
            return delegate.get(key);
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] get(" + key + ") failed: " + e.getMessage());
            return null; // Treat as cache miss
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            return delegate.get(key, type);
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] get(" + key + ", " + type + ") failed: " + e.getMessage());
            return null; // Treat as cache miss
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            return delegate.get(key, valueLoader);
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] get(" + key + ", valueLoader) failed: " + e.getMessage());
            // Execute the value loader directly on cache failure
            try {
                return valueLoader.call();
            } catch (Exception ex) {
                throw new RuntimeException("Value loader failed", ex);
            }
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            delegate.put(key, value);
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] put(" + key + ") failed: " + e.getMessage());
            // Silently ignore - app continues without caching
        }
    }

    @Override
    public void evict(Object key) {
        try {
            delegate.evict(key);
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] evict(" + key + ") failed: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        try {
            delegate.clear();
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] clear() failed: " + e.getMessage());
        }
    }

    @Override
    public boolean invalidate() {
        try {
            return delegate.invalidate();
        } catch (Exception e) {
            System.err.println("[CACHE_ERROR] invalidate() failed: " + e.getMessage());
            return false;
        }
    }
}
