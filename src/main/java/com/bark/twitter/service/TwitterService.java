package com.bark.twitter.service;

import com.bark.twitter.cache.UsernameCacheService;
import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.credits.CreditService;
import com.bark.twitter.dto.BatchUserResult;
import com.bark.twitter.dto.FollowsResponseDto;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NoCreditsException;
import com.bark.twitter.provider.TwitterDataProvider;
import com.bark.twitter.usage.DetailedUsageTrackingService;
import com.bark.twitter.usage.UsageTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwitterService {

    private final TwitterDataProvider dataProvider;
    private final VideoCacheWarmingService videoCacheWarmingService;
    private final DetailedUsageTrackingService detailedUsageTrackingService;
    private final UsageTrackingService usageTrackingService;
    private final CreditService creditService;
    private final UsernameCacheService usernameCacheService;
    private final LatencyTracker latencyTracker;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(TwitterDataProvider dataProvider,
                          VideoCacheWarmingService videoCacheWarmingService,
                          DetailedUsageTrackingService detailedUsageTrackingService,
                          UsageTrackingService usageTrackingService,
                          CreditService creditService,
                          UsernameCacheService usernameCacheService,
                          LatencyTracker latencyTracker,
                          CacheManager cacheManager) {
        this.dataProvider = dataProvider;
        this.videoCacheWarmingService = videoCacheWarmingService;
        this.detailedUsageTrackingService = detailedUsageTrackingService;
        this.usageTrackingService = usageTrackingService;
        this.creditService = creditService;
        this.usernameCacheService = usernameCacheService;
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

    /**
     * Batch fetches followers/following counts for multiple usernames.
     * Uses username cache (username→userId mapping) and data cache (30min TTL) to optimize.
     * Only fetches from Synoptic for users not in data cache.
     * Credits are only deducted for cache misses.
     */
    public FollowsResponseDto getFollowsByUsernames(List<String> usernames, String apiKey) {
        long start = System.currentTimeMillis();
        int totalHandles = usernames.size();

        // 1. Lookup userIds from username cache
        Map<String, String> cachedIds = usernameCacheService.getUserIds(usernames);
        int usernameCacheHits = cachedIds.size();
        int usernameCacheMisses = totalHandles - usernameCacheHits;

        // 2. Build response map and track what needs fetching
        Map<String, FollowsResponseDto.UserFollows> usersMap = new HashMap<>();
        List<String> notFoundList = new ArrayList<>();
        List<String> errorsList = new ArrayList<>();

        List<String> userIdsToFetch = new ArrayList<>();
        List<String> usernamesToFetch = new ArrayList<>();
        Map<String, String> lowerToOriginal = new HashMap<>(); // lowercase -> original case
        Map<String, String> userIdToOriginal = new HashMap<>(); // userId -> original case

        int dataCacheHits = 0;
        int dataCacheMisses = 0;

        for (String username : usernames) {
            String usernameLower = username.toLowerCase();
            String userId = cachedIds.get(usernameLower);

            if (userId != null) {
                // Have userId - check data cache by userId
                AxionUserInfoDto cached = usersCache.get(userId, AxionUserInfoDto.class);
                if (cached != null) {
                    usersMap.put(username, new FollowsResponseDto.UserFollows(cached.followers(), cached.following()));
                    dataCacheHits++;
                } else {
                    userIdsToFetch.add(userId);
                    userIdToOriginal.put(userId, username);
                    dataCacheMisses++;
                }
            } else {
                // No userId - check data cache by username
                String cacheKey = "username:" + usernameLower;
                AxionUserInfoDto cached = usersCache.get(cacheKey, AxionUserInfoDto.class);
                if (cached != null) {
                    usersMap.put(username, new FollowsResponseDto.UserFollows(cached.followers(), cached.following()));
                    dataCacheHits++;
                } else {
                    usernamesToFetch.add(usernameLower);
                    lowerToOriginal.put(usernameLower, username);
                    dataCacheMisses++;
                }
            }
        }

        // 3. Check and deduct credits only for cache misses
        if (dataCacheMisses > 0) {
            if (!creditService.decrementCredits(apiKey, dataCacheMisses)) {
                throw new NoCreditsException("Insufficient credits for " + dataCacheMisses + " handles");
            }
        }

        // 4. Record usage (only for cache misses)
        usageTrackingService.recordCalls(apiKey, "/follows", dataCacheMisses);

        // 5. Fetch uncached users via provider
        long fetchStart = System.currentTimeMillis();
        BatchUserResult byIdResult = dataProvider.getUsersByIds(userIdsToFetch);
        BatchUserResult byUsernameResult = dataProvider.getUsersByUsernames(usernamesToFetch);
        long fetchDuration = System.currentTimeMillis() - fetchStart;

        // Track latency for Synoptic fetches (only if we actually fetched something)
        if (!userIdsToFetch.isEmpty() || !usernamesToFetch.isEmpty()) {
            latencyTracker.recordCacheMiss("/follows", fetchDuration);
        }

        // 6. Process results from ID lookups - add to response and cache
        for (Map.Entry<String, AxionUserInfoDto> entry : byIdResult.getFound().entrySet()) {
            String userId = entry.getKey();
            AxionUserInfoDto user = entry.getValue();
            String originalCase = userIdToOriginal.get(userId);
            if (originalCase != null) {
                usersMap.put(originalCase, new FollowsResponseDto.UserFollows(user.followers(), user.following()));
                // Cache by userId
                usersCache.put(userId, user);
            }
        }

        for (String userId : byIdResult.getNotFound()) {
            String originalCase = userIdToOriginal.get(userId);
            if (originalCase != null) {
                notFoundList.add(originalCase);
            }
        }

        for (String userId : byIdResult.getErrors()) {
            String originalCase = userIdToOriginal.get(userId);
            if (originalCase != null) {
                errorsList.add(originalCase);
            }
        }

        // 7. Process results from username lookups - add to response and cache
        for (Map.Entry<String, AxionUserInfoDto> entry : byUsernameResult.getFound().entrySet()) {
            String usernameLower = entry.getKey();
            AxionUserInfoDto user = entry.getValue();
            String originalCase = lowerToOriginal.get(usernameLower);
            if (originalCase != null) {
                usersMap.put(originalCase, new FollowsResponseDto.UserFollows(user.followers(), user.following()));
                // Cache by username
                usersCache.put("username:" + usernameLower, user);
            }
        }

        for (String usernameLower : byUsernameResult.getNotFound()) {
            String originalCase = lowerToOriginal.get(usernameLower);
            if (originalCase != null) {
                notFoundList.add(originalCase);
            }
        }

        for (String usernameLower : byUsernameResult.getErrors()) {
            String originalCase = lowerToOriginal.get(usernameLower);
            if (originalCase != null) {
                errorsList.add(originalCase);
            }
        }

        // 8. Count results for logging
        int synopticByIdCalls = userIdsToFetch.isEmpty() ? 0 : (int) Math.ceil((double) userIdsToFetch.size() / 100);
        int synopticByUsernameCalls = usernamesToFetch.size();
        int synopticFetched = byIdResult.getFound().size() + byUsernameResult.getFound().size();
        int notFoundCount = notFoundList.size();
        int errorCount = errorsList.size();

        // 9. Track detailed usage (synoptic calls = cost, data cache hits = 0 cost)
        detailedUsageTrackingService.recordApiCalls(apiKey, "/follows", synopticFetched);
        detailedUsageTrackingService.recordCacheHits(apiKey, "/follows", dataCacheHits);

        long elapsed = System.currentTimeMillis() - start;

        // 10. Log summary (request/response logs are in controller)
        System.out.println("[" + System.currentTimeMillis() + "][" + apiKey.substring(0, 8) + "][FOLLOWS][SUMMARY] " +
                "usernameCacheHits=" + usernameCacheHits + " usernameCacheMisses=" + usernameCacheMisses +
                " dataCacheHits=" + dataCacheHits + " dataCacheMisses=" + dataCacheMisses +
                " synopticByIdCalls=" + synopticByIdCalls + " synopticByUsernameCalls=" + synopticByUsernameCalls +
                " found=" + usersMap.size() + " notFound=" + notFoundCount + " errors=" + errorCount +
                " duration=" + elapsed + "ms");

        return new FollowsResponseDto(usersMap, notFoundList, errorsList);
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
