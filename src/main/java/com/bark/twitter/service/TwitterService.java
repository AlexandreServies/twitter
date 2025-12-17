package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.client.TwitterApiClient;
import com.bark.twitter.dto.twitterapi.AuthorDto;
import com.bark.twitter.dto.twitterapi.TweetDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.SynopticToTwitterApiMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;
    private final TwitterApiClient twitterApiClient;
    private final SynopticToTwitterApiMapper mapper;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(SynopticClient synopticClient, TwitterApiClient twitterApiClient,
                          SynopticToTwitterApiMapper mapper, CacheManager cacheManager) {
        this.synopticClient = synopticClient;
        this.twitterApiClient = twitterApiClient;
        this.mapper = mapper;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
    }

    public TweetDto getTweet(String tweetId) {
        TweetDto cached = tweetsCache.get(tweetId, TweetDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + tweetId + "] Cache hit");
            return cached;
        }

        JsonNode synopticTweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        JsonNode transformed = transformMedia(synopticTweet);
        JsonNode enriched = enrichReplyData(transformed);

        TweetDto tweetDto = mapper.mapTweet(enriched);
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

    public AuthorDto getUser(String userId) {
        AuthorDto cached = usersCache.get(userId, AuthorDto.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][USER][" + userId + "] Cache hit");
            return cached;
        }

        JsonNode synopticUser = synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        AuthorDto userDto = mapper.mapUser(synopticUser);
        usersCache.put(userId, userDto);
        return userDto;
    }

    public JsonNode getCommunity(String communityId) {
        JsonNode cached = communitiesCache.get(communityId, JsonNode.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][COMMUNITY][" + communityId + "] Cache hit");
            return cached;
        }

        JsonNode community = twitterApiClient.getCommunity(communityId)
                .orElseThrow(() -> new NotFoundException("Community not found: " + communityId));

        communitiesCache.put(communityId, community);
        return community;
    }
}
