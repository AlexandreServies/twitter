package com.bark.twitter.provider;

import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.provider.SourceHealthMonitor.Endpoint;
import com.bark.twitter.provider.SourceHealthMonitor.Source;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Smart data provider that orchestrates source selection, fallback, and shadowing.
 * Delegates to SynopticDataProvider or TwitterApiDataProvider based on health status.
 */
@Component
@Primary
public class SmartTwitterDataProvider implements TwitterDataProvider {

    private final SynopticDataProvider synopticProvider;
    private final TwitterApiDataProvider twitterApiProvider;
    private final SourceHealthMonitor healthMonitor;
    private final EndpointSourceManager sourceManager;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    @Value("${twitter.sources.tweet:TWITTERAPI}")
    private String tweetSourceConfig;

    @Value("${twitter.sources.user:TWITTERAPI}")
    private String userSourceConfig;

    @Value("${twitter.sources.community:TWITTERAPI}")
    private String communitySourceConfig;

    public SmartTwitterDataProvider(SynopticDataProvider synopticProvider,
                                    TwitterApiDataProvider twitterApiProvider,
                                    SourceHealthMonitor healthMonitor,
                                    EndpointSourceManager sourceManager,
                                    CacheManager cacheManager) {
        this.synopticProvider = synopticProvider;
        this.twitterApiProvider = twitterApiProvider;
        this.healthMonitor = healthMonitor;
        this.sourceManager = sourceManager;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
    }

    @PostConstruct
    public void init() {
        // Configure primary sources from YAML
        sourceManager.configurePrimarySource(Endpoint.TWEET, Source.valueOf(tweetSourceConfig));
        sourceManager.configurePrimarySource(Endpoint.USER, Source.valueOf(userSourceConfig));
        sourceManager.configurePrimarySource(Endpoint.COMMUNITY, Source.valueOf(communitySourceConfig));
    }

    @Override
    public AxionTweetDto getTweet(String tweetId) {
        return executeWithFallback(
                Endpoint.TWEET,
                tweetId,
                () -> synopticProvider.getTweet(tweetId),
                () -> twitterApiProvider.getTweet(tweetId),
                result -> tweetsCache.put(tweetId, result)
        );
    }

    @Override
    public AxionUserInfoDto getUser(String userId) {
        return executeWithFallback(
                Endpoint.USER,
                userId,
                () -> synopticProvider.getUser(userId),
                () -> twitterApiProvider.getUser(userId),
                result -> usersCache.put(userId, result)
        );
    }

    @Override
    public AxionUserInfoDto getUserByUsername(String username) {
        String cacheKey = "username:" + username;
        return executeWithFallback(
                Endpoint.USER,
                username,
                () -> synopticProvider.getUserByUsername(username),
                () -> twitterApiProvider.getUserByUsername(username),
                result -> usersCache.put(cacheKey, result)
        );
    }

    @Override
    public AxionCommunityDto getCommunity(String communityId) {
        return executeWithFallback(
                Endpoint.COMMUNITY,
                communityId,
                () -> synopticProvider.getCommunity(communityId),
                () -> twitterApiProvider.getCommunity(communityId),
                result -> communitiesCache.put(communityId, result)
        );
    }

    @Override
    public String getProviderName() {
        return "SMART";
    }

    /**
     * Executes a request with fallback and shadowing logic.
     */
    private <T> T executeWithFallback(Endpoint endpoint,
                                      String id,
                                      Supplier<T> synopticCall,
                                      Supplier<T> twitterApiCall,
                                      java.util.function.Consumer<T> cacheResultCallback) {
        Source activeSource = sourceManager.getActiveSource(endpoint);
        Source primarySource = sourceManager.getPrimarySource(endpoint);
        boolean shouldShadow = sourceManager.shouldShadow(endpoint);

        // Select the call based on active source
        Supplier<T> activeCall = activeSource == Source.SYNOPTIC ? synopticCall : twitterApiCall;
        Supplier<T> primaryCall = primarySource == Source.SYNOPTIC ? synopticCall : twitterApiCall;

        // Execute the active call with latency tracking
        long start = System.currentTimeMillis();
        T result;
        try {
            result = activeCall.get();
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordLatency(activeSource, endpoint, latency);
            System.out.println("[SMART][" + endpoint + "][" + id + "] " + activeSource + " success in " + latency + "ms");
        } catch (NotFoundException e) {
            // Not found is not an error for health purposes, just rethrow
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordLatency(activeSource, endpoint, latency);
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordError(activeSource, endpoint);
            System.out.println("[SMART][" + endpoint + "][" + id + "] " + activeSource + " ERROR in " + latency + "ms: " + e.getMessage());
            sourceManager.triggerHealthCheck(endpoint);
            throw e;
        }

        // Trigger health check after successful request too (for latency-based transitions)
        sourceManager.triggerHealthCheck(endpoint);

        // If in fallback/recovering state, shadow request to primary source async
        if (shouldShadow && activeSource != primarySource) {
            shadowRequestAsync(endpoint, id, primaryCall, cacheResultCallback);
        }

        return result;
    }

    /**
     * Executes a shadow request to the primary source asynchronously.
     * Results are cached to warm the cache for when we switch back.
     */
    @Async
    protected <T> void shadowRequestAsync(Endpoint endpoint,
                                          String id,
                                          Supplier<T> primaryCall,
                                          java.util.function.Consumer<T> cacheResultCallback) {
        Source primarySource = sourceManager.getPrimarySource(endpoint);
        long start = System.currentTimeMillis();
        try {
            T result = primaryCall.get();
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordLatency(primarySource, endpoint, latency);

            // Cache the result to warm the cache
            if (result != null && cacheResultCallback != null) {
                cacheResultCallback.accept(result);
            }

            System.out.println("[SHADOW][" + endpoint + "][" + id + "] " + primarySource + " success in " + latency + "ms");
        } catch (NotFoundException e) {
            // Not found is not an error for health purposes
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordLatency(primarySource, endpoint, latency);
            System.out.println("[SHADOW][" + endpoint + "][" + id + "] " + primarySource + " not found in " + latency + "ms");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            healthMonitor.recordError(primarySource, endpoint);
            System.out.println("[SHADOW][" + endpoint + "][" + id + "] " + primarySource + " ERROR in " + latency + "ms: " + e.getMessage());
        }

        // Trigger health check after shadow request
        sourceManager.triggerHealthCheck(endpoint);
    }

    /**
     * Returns current status of all endpoints for monitoring/debugging.
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Smart Provider Status ===\n");
        sb.append(sourceManager.getStatusSummary());
        sb.append("\n=== Health Metrics ===\n");
        for (Endpoint endpoint : Endpoint.values()) {
            for (Source source : Source.values()) {
                sb.append(healthMonitor.getHealthSummary(source, endpoint)).append("\n");
            }
        }
        return sb.toString();
    }
}
