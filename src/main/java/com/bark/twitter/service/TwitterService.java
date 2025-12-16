package com.bark.twitter.service;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
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
        return synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));
    }

    @Cacheable(value = "users", key = "#userId")
    public JsonNode getUser(String userId) {
        return synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    public JsonNode getCommunity(String communityId) {
        // Not implemented yet
        throw new UnsupportedOperationException("Community lookup not implemented");
    }
}
