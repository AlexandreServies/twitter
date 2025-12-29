package com.bark.twitter.provider;

import com.bark.twitter.client.TwitterApiClient;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.TwitterApiToAxionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Twitter data provider that uses TwitterAPI.io as the data source.
 */
@Component
public class TwitterApiDataProvider implements TwitterDataProvider {

    private final TwitterApiClient twitterApiClient;
    private final TwitterApiToAxionMapper axiomMapper;

    public TwitterApiDataProvider(TwitterApiClient twitterApiClient, TwitterApiToAxionMapper axiomMapper) {
        this.twitterApiClient = twitterApiClient;
        this.axiomMapper = axiomMapper;
    }

    @Override
    public AxionTweetDto getTweet(String tweetId) {
        JsonNode twitterApiTweet = twitterApiClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        return axiomMapper.mapTweet(twitterApiTweet);
    }

    @Override
    public AxionUserInfoDto getUser(String userId) {
        JsonNode twitterApiUser = twitterApiClient.getUser(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return axiomMapper.mapUser(twitterApiUser);
    }

    @Override
    public AxionUserInfoDto getUserByUsername(String username) {
        JsonNode twitterApiUser = twitterApiClient.getUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: @" + username));

        return axiomMapper.mapUser(twitterApiUser);
    }

    @Override
    public AxionCommunityDto getCommunity(String communityId) {
        JsonNode communityInfo = twitterApiClient.getCommunity(communityId)
                .orElseThrow(() -> new NotFoundException("Community not found: " + communityId));

        return mapCommunity(communityInfo);
    }

    @Override
    public String getProviderName() {
        return "TWITTERAPI";
    }

    /**
     * Maps TwitterAPI.io community response to Axion format.
     */
    private AxionCommunityDto mapCommunity(JsonNode communityInfo) {
        // Extract admin info for creator
        JsonNode admin = communityInfo.get("admin");

        return AxionCommunityDto.builder()
                .name(getText(communityInfo, "name"))
                .description(getText(communityInfo, "description"))
                .memberCount(getInt(communityInfo, "member_count"))
                .createdAt(getText(communityInfo, "created_at"))
                .primaryTopic(mapPrimaryTopic(communityInfo.get("primary_topic")))
                .bannerUrl(getText(communityInfo, "banner_url"))
                .creator(mapCreator(admin))
                .build();
    }

    private com.bark.twitter.dto.axion.AxionPrimaryTopicDto mapPrimaryTopic(JsonNode topicNode) {
        if (topicNode == null || topicNode.isNull()) {
            return com.bark.twitter.dto.axion.AxionPrimaryTopicDto.empty();
        }
        String name = getText(topicNode, "name");
        if (name.isEmpty()) {
            return com.bark.twitter.dto.axion.AxionPrimaryTopicDto.empty();
        }
        return new com.bark.twitter.dto.axion.AxionPrimaryTopicDto(name);
    }

    private com.bark.twitter.dto.axion.AxionCreatorDto mapCreator(JsonNode admin) {
        if (admin == null || admin.isNull()) {
            return null;
        }
        return com.bark.twitter.dto.axion.AxionCreatorDto.builder()
                .name(getText(admin, "name"))
                .screenName(getText(admin, "screen_name"))
                .description(getText(admin, "description"))
                .profileBannerUrl(getText(admin, "profile_banner_url"))
                .profileImageUrlHttps(getText(admin, "profile_image_url_https"))
                .isBlueVerified(getBool(admin, "is_blue_verified"))
                .verifiedType(getText(admin, "verified_type"))
                .followersCount(getInt(admin, "followers_count"))
                .followingCount(getInt(admin, "friends_count"))
                .build();
    }

    private String getText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("");
    }

    private int getInt(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return 0;
        }
        return node.get(field).asInt(0);
    }

    private boolean getBool(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return false;
        }
        return node.get(field).asBoolean(false);
    }
}
