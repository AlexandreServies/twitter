package com.bark.twitter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("tweets",
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.tweets().ttlMinutes(), TimeUnit.MINUTES)
                        .build());

        cacheManager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.users().ttlMinutes(), TimeUnit.MINUTES)
                        .build());

        cacheManager.registerCustomCache("communities",
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.communities().ttlMinutes(), TimeUnit.MINUTES)
                        .build());

        cacheManager.registerCustomCache("follows",
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.follows().ttlMinutes(), TimeUnit.MINUTES)
                        .build());

        return cacheManager;
    }
}
