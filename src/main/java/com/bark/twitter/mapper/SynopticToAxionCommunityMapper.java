package com.bark.twitter.mapper;

import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionCreatorDto;
import com.bark.twitter.dto.axion.AxionPrimaryTopicDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Mapper that transforms Synoptic community + user responses to Axion format.
 */
@Component
public class SynopticToAxionCommunityMapper {

    private static final DateTimeFormatter TWITTER_DATE_FORMAT = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    /**
     * Maps Synoptic community data and creator user data to Axion format.
     *
     * @param communityData Synoptic community response (the "data" object)
     * @param creatorData   Synoptic user response for creator (the first element of "data" array)
     * @return AxionCommunityDto
     */
    public AxionCommunityDto mapCommunity(JsonNode communityData, JsonNode creatorData) {
        return AxionCommunityDto.builder()
                .name(getText(communityData, "name"))
                .description(getText(communityData, "description"))
                .memberCount(getInt(communityData, "member_count"))
                .createdAt(formatCreatedAt(communityData.get("created_at")))
                .primaryTopic(mapPrimaryTopic(communityData.get("primary_topic")))
                .bannerUrl(getText(communityData, "banner_url"))
                .creator(mapCreator(creatorData))
                .membersPreview(List.of()) // Not available in Synoptic
                .build();
    }

    private String formatCreatedAt(JsonNode createdAtNode) {
        if (createdAtNode == null || createdAtNode.isNull()) {
            return "";
        }
        // Synoptic returns timestamp in milliseconds
        long timestamp = createdAtNode.asLong();
        Instant instant = Instant.ofEpochMilli(timestamp);
        return TWITTER_DATE_FORMAT.format(instant);
    }

    private AxionPrimaryTopicDto mapPrimaryTopic(JsonNode topicNode) {
        if (topicNode == null || topicNode.isNull()) {
            return AxionPrimaryTopicDto.empty();
        }
        String name = getText(topicNode, "name");
        return new AxionPrimaryTopicDto(name);
    }

    private AxionCreatorDto mapCreator(JsonNode creatorNode) {
        if (creatorNode == null || creatorNode.isNull()) {
            return null;
        }
        return AxionCreatorDto.builder()
                .name(getText(creatorNode, "name"))
                .screenName(getText(creatorNode, "screen_name"))
                .description(getText(creatorNode, "description"))
                .profileBannerUrl(getText(creatorNode, "profile_banner_url"))
                .profileImageUrlHttps(getText(creatorNode, "profile_image_url"))
                .isBlueVerified(getBool(creatorNode, "is_blue_verified"))
                .verifiedType(getText(creatorNode, "verified_type"))
                .followersCount(getInt(creatorNode, "followers_count"))
                .followingCount(getInt(creatorNode, "following_count"))
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
