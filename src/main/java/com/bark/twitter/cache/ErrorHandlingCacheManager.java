package com.bark.twitter.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

/**
 * CacheManager wrapper that returns error-handling caches.
 * Ensures the application continues to function even if Redis is down.
 */
public class ErrorHandlingCacheManager implements CacheManager {

    private final CacheManager delegate;

    public ErrorHandlingCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache != null ? new ErrorHandlingCache(cache) : null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
