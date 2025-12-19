package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.client.TwitterApiClient;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.SynopticToAxionMapper;
import com.bark.twitter.mapper.SynopticToTwitterApiMapper;
import com.bark.twitter.mapper.TwitterApiToAxionCommunityMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;
    private final TwitterApiClient twitterApiClient;
    private final SynopticToTwitterApiMapper twitterApiMapper;
    private final SynopticToAxionMapper axionMapper;
    private final TwitterApiToAxionCommunityMapper communityMapper;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(SynopticClient synopticClient, TwitterApiClient twitterApiClient,
                          SynopticToTwitterApiMapper twitterApiMapper, SynopticToAxionMapper axionMapper,
                          TwitterApiToAxionCommunityMapper communityMapper, CacheManager cacheManager) {
        this.synopticClient = synopticClient;
        this.twitterApiClient = twitterApiClient;
        this.twitterApiMapper = twitterApiMapper;
        this.axionMapper = axionMapper;
        this.communityMapper = communityMapper;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
    }

    public AxionTweetDto getTweet(String tweetId) {
        AxionTweetDto cached = tweetsCache.get(tweetId, AxionTweetDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + tweetId + "] Cache hit");
            return cached;
        }

        JsonNode synopticTweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        JsonNode transformed = transformMedia(synopticTweet);
        JsonNode enriched = enrichReplyData(transformed);

        AxionTweetDto tweetDto = axionMapper.mapTweet(enriched);
        tweetsCache.put(tweetId, tweetDto);
        return tweetDto;
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

    public AxionUserInfoDto getUser(String userId) {
        AxionUserInfoDto cached = usersCache.get(userId, AxionUserInfoDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][USER][" + userId + "] Cache hit");
            return cached;
        }

        JsonNode synopticUser = synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        AxionUserInfoDto userDto = axionMapper.mapUser(synopticUser);
        usersCache.put(userId, userDto);
        return userDto;
    }

    public AxionCommunityDto getCommunity(String communityId) {
        AxionCommunityDto cached = communitiesCache.get(communityId, AxionCommunityDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][COMMUNITY][" + communityId + "] Cache hit");
            return cached;
        }

        JsonNode communityInfo = twitterApiClient.getCommunity(communityId)
                .orElseThrow(() -> new NotFoundException("Community not found: " + communityId));

        AxionCommunityDto communityDto = communityMapper.mapCommunity(communityInfo);
        communitiesCache.put(communityId, communityDto);
        return communityDto;
    }
}
