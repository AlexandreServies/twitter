package com.bark.twitter.service;

import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.provider.TwitterDataProvider;
import com.bark.twitter.usage.DetailedUsageTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class TwitterService {

    private final TwitterDataProvider dataProvider;
    private final VideoCacheWarmingService videoCacheWarmingService;
    private final DetailedUsageTrackingService detailedUsageTrackingService;
    private final LatencyTracker latencyTracker;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(TwitterDataProvider dataProvider,
                          VideoCacheWarmingService videoCacheWarmingService,
                          DetailedUsageTrackingService detailedUsageTrackingService,
                          LatencyTracker latencyTracker,
                          CacheManager cacheManager) {
        this.dataProvider = dataProvider;
        this.videoCacheWarmingService = videoCacheWarmingService;
        this.detailedUsageTrackingService = detailedUsageTrackingService;
        this.latencyTracker = latencyTracker;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
    }

    public AxionTweetDto getTweet(String tweetId) {
        String apiKey = getCurrentApiKey();
        AxionTweetDto cached = tweetsCache.get(tweetId, AxionTweetDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][CACHE_HIT][TWEET]");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/tweet");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordApiCall(apiKey, "/tweet");
        }

        long start = System.currentTimeMillis();
        AxionTweetDto tweetDto = dataProvider.getTweet(tweetId);
        tweetsCache.put(tweetId, tweetDto);
        latencyTracker.recordCacheMiss("/tweet", System.currentTimeMillis() - start);

        // Warm video cache async (fire-and-forget, no latency impact)
        videoCacheWarmingService.warmCacheAsync(tweetDto);

        return tweetDto;
    }

    public AxionUserInfoDto getUser(String userId) {
        String apiKey = getCurrentApiKey();
        AxionUserInfoDto cached = usersCache.get(userId, AxionUserInfoDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][" + userId + "][CACHE_HIT][USER]");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/user");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordApiCall(apiKey, "/user");
        }

        long start = System.currentTimeMillis();
        AxionUserInfoDto userDto = dataProvider.getUser(userId);
        usersCache.put(userId, userDto);
        latencyTracker.recordCacheMiss("/user", System.currentTimeMillis() - start);
        return userDto;
    }

    public AxionCommunityDto getCommunity(String communityId) {
        String apiKey = getCurrentApiKey();
        AxionCommunityDto cached = communitiesCache.get(communityId, AxionCommunityDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][CACHE_HIT][COMMUNITY]");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/community");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordApiCall(apiKey, "/community");
        }

        long start = System.currentTimeMillis();
        AxionCommunityDto communityDto = dataProvider.getCommunity(communityId);
        communitiesCache.put(communityId, communityDto);
        latencyTracker.recordCacheMiss("/community", System.currentTimeMillis() - start);
        return communityDto;
    }

    public AxionUserInfoDto getUserByUsername(String username) {
        String apiKey = getCurrentApiKey();
        String cacheKey = "username:" + username;
        AxionUserInfoDto cached = usersCache.get(cacheKey, AxionUserInfoDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][CACHE_HIT][USER]");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/user");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordApiCall(apiKey, "/user");
        }

        long start = System.currentTimeMillis();
        AxionUserInfoDto userDto = dataProvider.getUserByUsername(username);
        usersCache.put(cacheKey, userDto);
        latencyTracker.recordCacheMiss("/user", System.currentTimeMillis() - start);
        return userDto;
    }

    private String getCurrentApiKey() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);
        }
        return null;
    }
}
