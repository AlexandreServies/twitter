package com.bark.twitter.provider;

import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.SynopticToAxiomMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Twitter data provider that uses Synoptic API as the data source.
 */
@Component
public class SynopticDataProvider implements TwitterDataProvider {

    private final SynopticClient synopticClient;
    private final SynopticToAxiomMapper axiomMapper;

    public SynopticDataProvider(SynopticClient synopticClient, SynopticToAxiomMapper axiomMapper) {
        this.synopticClient = synopticClient;
        this.axiomMapper = axiomMapper;
    }

    @Override
    public AxionTweetDto getTweet(String tweetId) {
        JsonNode synopticTweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        JsonNode transformed = transformMedia(synopticTweet);
        JsonNode enriched = enrichReplyData(transformed);

        return axiomMapper.mapTweet(enriched);
    }

    @Override
    public AxionUserInfoDto getUser(String userId) {
        JsonNode synopticUser = synopticClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return axiomMapper.mapUser(synopticUser);
    }

    @Override
    public AxionUserInfoDto getUserByUsername(String username) {
        JsonNode synopticUser = synopticClient.getUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: @" + username));

        return axiomMapper.mapUser(synopticUser);
    }

    @Override
    public AxionCommunityDto getCommunity(String communityId) {
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

        return axiomMapper.mapCommunity(communityData, creatorData);
    }

    @Override
    public String getProviderName() {
        return "SYNOPTIC";
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
        JsonNode repliedToTweet = fetchRawTweet(repliedToTweetId);

        if (repliedToTweet != null) {
            ((ObjectNode) tweet).set("reply", repliedToTweet);
        }

        return tweet;
    }

    private JsonNode fetchRawTweet(String tweetId) {
        JsonNode tweet = synopticClient.getTweet(tweetId).orElse(null);
        if (tweet != null) {
            tweet = transformMedia(tweet);
        }
        return tweet;
    }
}
