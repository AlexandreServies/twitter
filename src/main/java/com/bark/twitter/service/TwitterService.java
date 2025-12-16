package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;
    private final Cache tweetsCache;
    private final Cache usersCache;
    private final Cache communitiesCache;

    public TwitterService(SynopticClient synopticClient, CacheManager cacheManager) {
        this.synopticClient = synopticClient;
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

        JsonNode enriched = enrichReplyData(tweet);
        tweetsCache.put(tweetId, enriched);
        return enriched;
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
        JsonNode repliedToTweet = synopticClient.getTweet(repliedToTweetId).orElse(null);

        if (repliedToTweet != null) {
            ((ObjectNode) tweet).set("reply", repliedToTweet);
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

        // Not implemented yet
        throw new UnsupportedOperationException("Community lookup not implemented");
    }
}
