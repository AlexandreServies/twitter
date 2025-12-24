package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.SynopticToAxiomMapper;
import com.bark.twitter.usage.DetailedUsageTrackingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;
    private final SynopticToAxiomMapper axiomMapper;
    private final VideoCacheWarmingService videoCacheWarmingService;
    private final DetailedUsageTrackingService detailedUsageTrackingService;
    private final LatencyTracker latencyTracker;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(SynopticClient synopticClient,
                          SynopticToAxiomMapper axiomMapper,
                          VideoCacheWarmingService videoCacheWarmingService,
                          DetailedUsageTrackingService detailedUsageTrackingService,
                          LatencyTracker latencyTracker,
                          CacheManager cacheManager) {
        this.synopticClient = synopticClient;
        this.axiomMapper = axiomMapper;
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
            System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + tweetId + "] Cache hit");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/tweet");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordSynopticCall(apiKey, "/tweet");
        }

        long start = System.currentTimeMillis();
        JsonNode synopticTweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        JsonNode transformed = transformMedia(synopticTweet);
        JsonNode enriched = enrichReplyData(transformed);

        AxionTweetDto tweetDto = axiomMapper.mapTweet(enriched);
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
            System.out.println("[" + System.currentTimeMillis() + "][USER][" + userId + "] Cache hit");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/user");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordSynopticCall(apiKey, "/user");
        }

        long start = System.currentTimeMillis();
        JsonNode synopticUser = synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        AxionUserInfoDto userDto = axiomMapper.mapUser(synopticUser);
        usersCache.put(userId, userDto);
        latencyTracker.recordCacheMiss("/user", System.currentTimeMillis() - start);
        return userDto;
    }

    public AxionCommunityDto getCommunity(String communityId) {
        String apiKey = getCurrentApiKey();
        AxionCommunityDto cached = communitiesCache.get(communityId, AxionCommunityDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][COMMUNITY][" + communityId + "] Cache hit");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/community");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordSynopticCall(apiKey, "/community");
        }

        long start = System.currentTimeMillis();

        // First call: get community data
        JsonNode communityData = synopticClient.getCommunity(communityId)
                .orElseThrow(() -> new NotFoundException("Community not found: " + communityId));

        // Extract creator user ID from community data
        JsonNode creatorNode = communityData.get("creator");
        String creatorUserId = null;
        if (creatorNode != null && !creatorNode.isNull()) {
            JsonNode userIdNode = creatorNode.get("user_id");
            if (userIdNode != null && !userIdNode.isNull()) {
                creatorUserId = userIdNode.asText();
            }
        }

        // Second call: get creator user data
        JsonNode creatorData = null;
        if (creatorUserId != null) {
            creatorData = synopticClient.getUser(creatorUserId).orElse(null);
        }

        AxionCommunityDto communityDto = axiomMapper.mapCommunity(communityData, creatorData);
        communitiesCache.put(communityId, communityDto);
        latencyTracker.recordCacheMiss("/community", System.currentTimeMillis() - start);
        return communityDto;
    }

    private JsonNode transformMedia(JsonNode tweet) {
        if (!(tweet instanceof ObjectNode node)) {
            return tweet;
        }
        node.remove("media");
        JsonNode mediaV2 = node.remove("mediaV2");
        if (mediaV2 != null && !mediaV2.isNull()) {
            node.set("media", mediaV2);
        }
        return node;
    }

    private JsonNode enrichReplyData(JsonNode tweet) {
        JsonNode replyNode = tweet.get("reply");
        if (replyNode == null || replyNode.isNull()) {
            return tweet;
        }

        JsonNode replyToStatusId = replyNode.get("reply_to_status_id");
        if (replyToStatusId == null || replyToStatusId.isNull()) {
            return tweet;
        }

        String repliedToTweetId = replyToStatusId.asText();
        JsonNode repliedToTweet = fetchRawTweetWithCache(repliedToTweetId);

        if (repliedToTweet != null) {
            ((ObjectNode) tweet).set("reply", repliedToTweet);
        }

        return tweet;
    }

    private JsonNode fetchRawTweetWithCache(String tweetId) {
        // Check if we have the raw tweet data
        JsonNode tweet = synopticClient.getTweet(tweetId).orElse(null);
        if (tweet != null) {
            tweet = transformMedia(tweet);
        }
        return tweet;
    }

    public AxionUserInfoDto getUserByUsername(String username) {
        String apiKey = getCurrentApiKey();
        String cacheKey = "username:" + username;
        AxionUserInfoDto cached = usersCache.get(cacheKey, AxionUserInfoDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][USER][@" + username + "] Cache hit");
            if (apiKey != null) {
                detailedUsageTrackingService.recordCacheHit(apiKey, "/user");
            }
            return cached;
        }

        if (apiKey != null) {
            detailedUsageTrackingService.recordSynopticCall(apiKey, "/user");
        }

        long start = System.currentTimeMillis();
        JsonNode synopticUser = synopticClient.getUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: @" + username));

        AxionUserInfoDto userDto = axiomMapper.mapUser(synopticUser);
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
