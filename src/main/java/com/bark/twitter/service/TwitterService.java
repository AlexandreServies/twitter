package com.bark.twitter.service;

import com.bark.twitter.cache.CachedData;
import com.bark.twitter.cache.UsernameCacheService;
import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.config.CacheProperties;
import com.bark.twitter.credits.CreditService;
import com.bark.twitter.dto.BatchCommunityMemberCountResult;
import com.bark.twitter.dto.BatchUserResult;
import com.bark.twitter.dto.CommunityDataDto;
import com.bark.twitter.dto.CommunityMemberCountsResponseDto;
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
import java.util.function.Supplier;

@Service
public class TwitterService {

    public record FollowsResult(FollowsResponseDto response, boolean hadCacheMisses, int billableCount) {}
    public record CommunityMemberCountsResult(CommunityMemberCountsResponseDto response, int billableCount) {}

    private final TwitterDataProvider dataProvider;
    private final VideoCacheWarmingService videoCacheWarmingService;
    private final DetailedUsageTrackingService detailedUsageTrackingService;
    private final UsageTrackingService usageTrackingService;
    private final CreditService creditService;
    private final UsernameCacheService usernameCacheService;
    private final LatencyTracker latencyTracker;
    private final CacheProperties cacheProperties;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;
    private final Cache followsCache;
    private final Cache communityMemberCountsCache;

    public TwitterService(TwitterDataProvider dataProvider,
                          VideoCacheWarmingService videoCacheWarmingService,
                          DetailedUsageTrackingService detailedUsageTrackingService,
                          UsageTrackingService usageTrackingService,
                          CreditService creditService,
                          UsernameCacheService usernameCacheService,
                          LatencyTracker latencyTracker,
                          CacheProperties cacheProperties,
                          CacheManager cacheManager) {
        this.dataProvider = dataProvider;
        this.videoCacheWarmingService = videoCacheWarmingService;
        this.detailedUsageTrackingService = detailedUsageTrackingService;
        this.usageTrackingService = usageTrackingService;
        this.creditService = creditService;
        this.usernameCacheService = usernameCacheService;
        this.latencyTracker = latencyTracker;
        this.cacheProperties = cacheProperties;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
        this.followsCache = cacheManager.getCache("follows");
        this.communityMemberCountsCache = cacheManager.getCache("community-member-counts");
    }

    public AxionTweetDto getTweet(String tweetId) {
        AxionTweetDto result = getWithBilling(
                tweetsCache,
                tweetId,
                "/tweet",
                cacheProperties.tweets().billingPeriodMs(),
                () -> dataProvider.getTweet(tweetId)
        );
        // Warm video cache async (fire-and-forget, no latency impact)
        videoCacheWarmingService.warmCacheAsync(result);
        return result;
    }

    public AxionUserInfoDto getUser(String userId) {
        return getWithBilling(
                usersCache,
                userId,
                "/user",
                cacheProperties.users().billingPeriodMs(),
                () -> dataProvider.getUser(userId)
        );
    }

    public AxionCommunityDto getCommunity(String communityId) {
        return getWithBilling(
                communitiesCache,
                communityId,
                "/community",
                cacheProperties.communities().billingPeriodMs(),
                () -> dataProvider.getCommunity(communityId)
        );
    }

    public AxionUserInfoDto getUserByUsername(String username) {
        return getWithBilling(
                usersCache,
                "username:" + username,
                "/user",
                cacheProperties.users().billingPeriodMs(),
                () -> dataProvider.getUserByUsername(username)
        );
    }

    /**
     * Batch fetches followers/following counts for multiple usernames.
     * Uses username cache (username→userId mapping) and follows cache with billing period.
     * Data is cached for TTL, but credits are only charged when billing period expires.
     */
    @SuppressWarnings("unchecked")
    public FollowsResult getFollowsByUsernames(List<String> usernames, String apiKey) {
        long start = System.currentTimeMillis();
        int totalHandles = usernames.size();
        long billingPeriodMs = cacheProperties.follows().billingPeriodMs();

        // 1. Lookup userIds from username cache
        Map<String, String> cachedIds = usernameCacheService.getUserIds(usernames);
        int usernameCacheHits = cachedIds.size();
        int usernameCacheMisses = totalHandles - usernameCacheHits;

        // 2. Build response map and track what needs fetching/billing
        Map<String, FollowsResponseDto.UserFollows> usersMap = new HashMap<>();
        List<String> notFoundList = new ArrayList<>();
        List<String> errorsList = new ArrayList<>();

        List<String> userIdsToFetch = new ArrayList<>();
        List<String> usernamesToFetch = new ArrayList<>();
        Map<String, String> lowerToOriginal = new HashMap<>(); // lowercase -> original case
        Map<String, String> userIdToOriginal = new HashMap<>(); // userId -> original case

        // Track cache entries that need billing update (cache hit but billing expired)
        List<String> cacheKeysNeedingBillingUpdate = new ArrayList<>();
        Map<String, CachedData<AxionUserInfoDto>> cachedEntriesForBillingUpdate = new HashMap<>();

        int dataCacheHits = 0;
        int dataCacheMisses = 0;
        int billableCount = 0;

        for (String username : usernames) {
            String usernameLower = username.toLowerCase();
            String userId = cachedIds.get(usernameLower);
            String cacheKey = userId != null ? userId : "username:" + usernameLower;

            CachedData<AxionUserInfoDto> cached = (CachedData<AxionUserInfoDto>) followsCache.get(cacheKey, CachedData.class);

            if (cached != null) {
                // Cache hit - check if billable
                usersMap.put(username, new FollowsResponseDto.UserFollows(cached.data().followers(), cached.data().following()));
                dataCacheHits++;

                if (cached.isBillable(billingPeriodMs)) {
                    billableCount++;
                    cacheKeysNeedingBillingUpdate.add(cacheKey);
                    cachedEntriesForBillingUpdate.put(cacheKey, cached);
                }
            } else {
                // Cache miss - need to fetch
                dataCacheMisses++;
                billableCount++;
                if (userId != null) {
                    userIdsToFetch.add(userId);
                    userIdToOriginal.put(userId, username);
                } else {
                    usernamesToFetch.add(usernameLower);
                    lowerToOriginal.put(usernameLower, username);
                }
            }
        }

        // 3. Check and deduct credits for billable requests (cache misses + expired billing window hits)
        if (billableCount > 0) {
            if (!creditService.decrementCredits(apiKey, billableCount)) {
                throw new NoCreditsException("Insufficient credits for " + billableCount + " handles");
            }
        }

        // 4. Update billing timestamps for cache hits that were billed
        for (String cacheKey : cacheKeysNeedingBillingUpdate) {
            CachedData<AxionUserInfoDto> cached = cachedEntriesForBillingUpdate.get(cacheKey);
            followsCache.put(cacheKey, cached.withUpdatedBilling());
        }

        // 5. Record usage (only for billable requests)
        usageTrackingService.recordCalls(apiKey, "/follows", billableCount);

        // 6. Fetch uncached users via provider
        long fetchStart = System.currentTimeMillis();
        BatchUserResult byIdResult = dataProvider.getUsersByIds(userIdsToFetch);
        BatchUserResult byUsernameResult = dataProvider.getUsersByUsernames(usernamesToFetch);
        long fetchDuration = System.currentTimeMillis() - fetchStart;

        // Track latency for Synoptic fetches (only if we actually fetched something)
        if (!userIdsToFetch.isEmpty() || !usernamesToFetch.isEmpty()) {
            latencyTracker.recordCacheMiss("/follows", fetchDuration);
        }

        // 7. Process results from ID lookups - add to response and cache
        for (Map.Entry<String, AxionUserInfoDto> entry : byIdResult.getFound().entrySet()) {
            String userId = entry.getKey();
            AxionUserInfoDto user = entry.getValue();
            String originalCase = userIdToOriginal.get(userId);
            if (originalCase != null) {
                usersMap.put(originalCase, new FollowsResponseDto.UserFollows(user.followers(), user.following()));
                followsCache.put(userId, CachedData.of(user));
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

        // 8. Process results from username lookups - add to response and cache
        for (Map.Entry<String, AxionUserInfoDto> entry : byUsernameResult.getFound().entrySet()) {
            String usernameLower = entry.getKey();
            AxionUserInfoDto user = entry.getValue();
            String originalCase = lowerToOriginal.get(usernameLower);
            if (originalCase != null) {
                usersMap.put(originalCase, new FollowsResponseDto.UserFollows(user.followers(), user.following()));
                followsCache.put("username:" + usernameLower, CachedData.of(user));
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

        // 9. Count results for logging
        int synopticByIdCalls = userIdsToFetch.isEmpty() ? 0 : (int) Math.ceil((double) userIdsToFetch.size() / 100);
        int synopticByUsernameCalls = usernamesToFetch.size();
        int synopticFetched = byIdResult.getFound().size() + byUsernameResult.getFound().size();
        int notFoundCount = notFoundList.size();
        int errorCount = errorsList.size();

        // 10. Track detailed usage
        // - hit: cache hits (served from cache)
        // - miss: cache misses (had to call Synoptic)
        // - found: items Synoptic actually found and charged for
        detailedUsageTrackingService.recordCacheHits(apiKey, "/follows", dataCacheHits);
        detailedUsageTrackingService.recordCacheMisses(apiKey, "/follows", dataCacheMisses);
        detailedUsageTrackingService.recordFoundItems(apiKey, "/follows", synopticFetched);

        long elapsed = System.currentTimeMillis() - start;

        // 11. Log summary only if there were cache misses
        if (dataCacheMisses > 0) {
            System.out.println("[" + System.currentTimeMillis() + "][" + apiKey.substring(0, 8) + "][FOLLOWS][SUMMARY] " +
                    "usernameCacheHits=" + usernameCacheHits + " usernameCacheMisses=" + usernameCacheMisses +
                    " dataCacheHits=" + dataCacheHits + " dataCacheMisses=" + dataCacheMisses +
                    " billable=" + billableCount +
                    " synopticByIdCalls=" + synopticByIdCalls + " synopticByUsernameCalls=" + synopticByUsernameCalls +
                    " found=" + usersMap.size() + " notFound=" + notFoundCount + " errors=" + errorCount +
                    " duration=" + elapsed + "ms");
        }

        return new FollowsResult(new FollowsResponseDto(usersMap, notFoundList, errorsList), dataCacheMisses > 0, billableCount);
    }

    /**
     * Batch fetches community member counts for multiple community IDs.
     * Data is cached for TTL, but credits are only charged when billing period expires.
     */
    @SuppressWarnings("unchecked")
    public CommunityMemberCountsResult getCommunityMemberCounts(List<String> communityIds, String apiKey) {
        long start = System.currentTimeMillis();
        long billingPeriodMs = cacheProperties.communityMemberCounts().billingPeriodMs();

        // Build response map and track what needs fetching/billing
        Map<String, CommunityDataDto> communitiesMap = new HashMap<>();
        List<String> notFoundList = new ArrayList<>();
        List<String> errorsList = new ArrayList<>();
        List<String> idsToFetch = new ArrayList<>();

        // Track cache entries that need billing update (cache hit but billing expired)
        List<String> cacheKeysNeedingBillingUpdate = new ArrayList<>();
        Map<String, CachedData<Long>> cachedEntriesForBillingUpdate = new HashMap<>();

        int dataCacheHits = 0;
        int dataCacheMisses = 0;
        int billableCount = 0;

        for (String communityId : communityIds) {
            CachedData<Long> cached = (CachedData<Long>) communityMemberCountsCache.get(communityId, CachedData.class);

            if (cached != null) {
                // Cache hit - check if billable
                communitiesMap.put(communityId, new CommunityDataDto(cached.data()));
                dataCacheHits++;

                if (cached.isBillable(billingPeriodMs)) {
                    billableCount++;
                    cacheKeysNeedingBillingUpdate.add(communityId);
                    cachedEntriesForBillingUpdate.put(communityId, cached);
                }
            } else {
                // Cache miss - need to fetch
                dataCacheMisses++;
                billableCount++;
                idsToFetch.add(communityId);
            }
        }

        // Check and deduct credits for billable requests
        if (billableCount > 0) {
            if (!creditService.decrementCredits(apiKey, billableCount)) {
                throw new NoCreditsException("Insufficient credits for " + billableCount + " communities");
            }
        }

        // Update billing timestamps for cache hits that were billed
        for (String cacheKey : cacheKeysNeedingBillingUpdate) {
            CachedData<Long> cached = cachedEntriesForBillingUpdate.get(cacheKey);
            communityMemberCountsCache.put(cacheKey, cached.withUpdatedBilling());
        }

        // Record usage (only for billable requests)
        usageTrackingService.recordCalls(apiKey, "/communities", billableCount);

        // Fetch uncached communities via provider
        long fetchStart = System.currentTimeMillis();
        BatchCommunityMemberCountResult fetchResult = dataProvider.getCommunityMemberCounts(idsToFetch);
        long fetchDuration = System.currentTimeMillis() - fetchStart;

        // Track latency for Synoptic fetches (only if we actually fetched something)
        if (!idsToFetch.isEmpty()) {
            latencyTracker.recordCacheMiss("/communities", fetchDuration);
        }

        // Process results - add to response and cache
        for (Map.Entry<String, Long> entry : fetchResult.getFound().entrySet()) {
            String communityId = entry.getKey();
            Long memberCount = entry.getValue();
            communitiesMap.put(communityId, new CommunityDataDto(memberCount));
            communityMemberCountsCache.put(communityId, CachedData.of(memberCount));
        }

        notFoundList.addAll(fetchResult.getNotFound());
        errorsList.addAll(fetchResult.getErrors());

        // Track detailed usage
        int synopticFetched = fetchResult.getFound().size();
        detailedUsageTrackingService.recordCacheHits(apiKey, "/communities", dataCacheHits);
        detailedUsageTrackingService.recordCacheMisses(apiKey, "/communities", dataCacheMisses);
        detailedUsageTrackingService.recordFoundItems(apiKey, "/communities", synopticFetched);

        long elapsed = System.currentTimeMillis() - start;

        // Log summary only if there were cache misses
        if (dataCacheMisses > 0) {
            System.out.println("[" + System.currentTimeMillis() + "][" + apiKey.substring(0, 8) + "][COMMUNITIES][SUMMARY] " +
                    "dataCacheHits=" + dataCacheHits + " dataCacheMisses=" + dataCacheMisses +
                    " billable=" + billableCount +
                    " synopticCalls=" + idsToFetch.size() +
                    " found=" + communitiesMap.size() + " notFound=" + notFoundList.size() + " errors=" + errorsList.size() +
                    " duration=" + elapsed + "ms");
        }

        return new CommunityMemberCountsResult(
                new CommunityMemberCountsResponseDto(communitiesMap, notFoundList, errorsList),
                billableCount
        );
    }

    /**
     * Generic helper for single-item endpoints with unified billing logic.
     * Handles cache lookup, billing period checks, credit deduction, and usage tracking.
     */
    @SuppressWarnings("unchecked")
    private <T> T getWithBilling(Cache cache, String cacheKey, String endpoint, long billingPeriodMs, Supplier<T> fetcher) {
        String apiKey = getCurrentApiKey();

        CachedData<T> cached = (CachedData<T>) cache.get(cacheKey, CachedData.class);

        if (cached != null) {
            // Cache hit - check if billable
            boolean billable = cached.isBillable(billingPeriodMs);

            if (billable) {
                // Billing period expired - charge and update billing timestamp
                if (apiKey != null) {
                    if (!creditService.decrementCredit(apiKey)) {
                        throw new NoCreditsException("No credits remaining");
                    }
                    usageTrackingService.recordCall(apiKey, endpoint);
                    cache.put(cacheKey, cached.withUpdatedBilling());
                    detailedUsageTrackingService.recordCacheHit(apiKey, endpoint);
                }
            } else {
                // Within billing period - free
                if (apiKey != null) {
                    detailedUsageTrackingService.recordCacheHit(apiKey, endpoint);
                }
            }

            System.out.println("[" + System.currentTimeMillis() + "][" + cacheKey + "][CACHE_HIT][" + endpoint.substring(1).toUpperCase() + "]" +
                    (billable ? "[BILLED]" : "[FREE]"));
            return cached.data();
        }

        // Cache miss - always billable
        if (apiKey != null) {
            if (!creditService.decrementCredit(apiKey)) {
                throw new NoCreditsException("No credits remaining");
            }
            usageTrackingService.recordCall(apiKey, endpoint);
            detailedUsageTrackingService.recordCacheMiss(apiKey, endpoint);
        }

        long start = System.currentTimeMillis();
        T data = fetcher.get();
        cache.put(cacheKey, CachedData.of(data));
        latencyTracker.recordCacheMiss(endpoint, System.currentTimeMillis() - start);

        // Only record "found" if data was returned (Synoptic doesn't charge for not-found)
        if (apiKey != null && data != null) {
            detailedUsageTrackingService.recordFound(apiKey, endpoint);
        }

        return data;
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
