package com.bark.twitter.mapper;

import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionCreatorDto;
import com.bark.twitter.dto.axion.AxionMemberPreviewDto;
import com.bark.twitter.dto.axion.AxionPrimaryTopicDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper that transforms TwitterAPI community responses to Axion format.
 */
@Component
public class TwitterApiToAxionCommunityMapper {

    public AxionCommunityDto mapCommunity(JsonNode communityInfo) {
        return AxionCommunityDto.builder()
                .name(getText(communityInfo, "name"))
                .description(getText(communityInfo, "description"))
                .memberCount(getInt(communityInfo, "member_count"))
                .createdAt(getText(communityInfo, "created_at"))
                .primaryTopic(mapPrimaryTopic(communityInfo.get("primary_topic")))
                .bannerUrl(getText(communityInfo, "banner_url"))
                .creator(mapCreator(communityInfo.get("creator")))
                .membersPreview(mapMembersPreview(communityInfo.get("members_preview")))
                .build();
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
                .profileImageUrlHttps(getText(creatorNode, "profile_image_url_https"))
                .isBlueVerified(getBool(creatorNode, "isBlueVerified"))
                .verifiedType(getText(creatorNode, "verified_type"))
                .followersCount(getInt(creatorNode, "followers_count"))
                .followingCount(getInt(creatorNode, "following_count"))
                .build();
    }

    private List<AxionMemberPreviewDto> mapMembersPreview(JsonNode membersNode) {
        List<AxionMemberPreviewDto> members = new ArrayList<>();
        if (membersNode == null || !membersNode.isArray()) {
            return members;
        }
        for (JsonNode memberNode : membersNode) {
            members.add(new AxionMemberPreviewDto(
                    getText(memberNode, "profile_image_url_https"),
                    getBool(memberNode, "isBlueVerified"),
                    getInt(memberNode, "followers_count")
            ));
        }
        return members;
    }

    private String getText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("");
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
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
