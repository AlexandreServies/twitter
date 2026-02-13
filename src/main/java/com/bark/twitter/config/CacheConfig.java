package com.bark.twitter.config;

import com.bark.twitter.cache.ErrorHandlingCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Redis cache manager - used when REDIS_HOST environment variable is set.
     * Provides shared caching across all instances.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        System.out.println("[CACHE] Using Redis cache manager");

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("tweets", defaultConfig.entryTtl(
                Duration.ofMinutes(cacheProperties.tweets().ttlMinutes())));

        cacheConfigs.put("users", defaultConfig.entryTtl(
                Duration.ofMinutes(cacheProperties.users().ttlMinutes())));

        cacheConfigs.put("communities", defaultConfig.entryTtl(
                Duration.ofMinutes(cacheProperties.communities().ttlMinutes())));

        cacheConfigs.put("follows", defaultConfig.entryTtl(
                Duration.ofMinutes(cacheProperties.follows().ttlMinutes())));

        cacheConfigs.put("community-member-counts", defaultConfig.entryTtl(
                Duration.ofMinutes(cacheProperties.communityMemberCounts().ttlMinutes())));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();

        // Wrap with error handling to ensure app continues if Redis is down
        return new ErrorHandlingCacheManager(redisCacheManager);
    }

    /**
     * Caffeine cache manager - fallback for local development without Redis.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager caffeineCacheManager() {
        System.out.println("[CACHE] Using Caffeine cache manager (local/fallback)");

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

        cacheManager.registerCustomCache("community-member-counts",
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.communityMemberCounts().ttlMinutes(), TimeUnit.MINUTES)
                        .build());

        return cacheManager;
    }
}
