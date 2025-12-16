package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private final SynopticClient synopticClient;

    public TwitterService(SynopticClient synopticClient) {
        this.synopticClient = synopticClient;
    }

    @Cacheable(value = "tweets", key = "#tweetId")
    public JsonNode getTweet(String tweetId) {
        JsonNode tweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        return enrichReplyData(tweet);
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

    @Cacheable(value = "users", key = "#userId")
    public JsonNode getUser(String userId) {
        return synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    @Cacheable(value = "communities", key = "#communityId")
    public JsonNode getCommunity(String communityId) {
        // Not implemented yet
        throw new UnsupportedOperationException("Community lookup not implemented");
    }
}
