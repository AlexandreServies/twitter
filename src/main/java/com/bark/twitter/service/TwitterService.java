package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.client.TwitterApiClient;
import com.bark.twitter.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;
    private final TwitterApiClient twitterApiClient;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(SynopticClient synopticClient, TwitterApiClient twitterApiClient, CacheManager cacheManager) {
        this.synopticClient = synopticClient;
        this.twitterApiClient = twitterApiClient;
        this.tweetsCache = cacheManager.getCache("tweets");
        this.usersCache = cacheManager.getCache("users");
        this.communitiesCache = cacheManager.getCache("communities");
    }

    public JsonNode getTweet(String tweetId) {
        JsonNode cached = tweetsCache.get(tweetId, JsonNode.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + tweetId + "] Cache hit");
            return cached;
        }

        JsonNode tweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        JsonNode transformed = transformMedia(tweet);
        JsonNode enriched = enrichReplyData(transformed);
        tweetsCache.put(tweetId, enriched);
        return enriched;
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
        JsonNode repliedToTweet = fetchTweetWithCache(repliedToTweetId);

        if (repliedToTweet != null) {
            ((ObjectNode) tweet).set("reply", repliedToTweet);
        }

        return tweet;
    }

    private JsonNode fetchTweetWithCache(String tweetId) {
        JsonNode cached = tweetsCache.get(tweetId, JsonNode.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + tweetId + "] Cache hit");
            return cached;
        }

        JsonNode tweet = synopticClient.getTweet(tweetId).orElse(null);
        if (tweet != null) {
            tweet = transformMedia(tweet);
            tweetsCache.put(tweetId, tweet);
        }
        return tweet;
    }

    public JsonNode getUser(String userId) {
        JsonNode cached = usersCache.get(userId, JsonNode.class);
        if (cached != null) {
            System.out.println("[" + System.currentTimeMillis() + "][USER][" + userId + "] Cache hit");
            return cached;
        }

        JsonNode user = synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        usersCache.put(userId, user);
        return user;
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
