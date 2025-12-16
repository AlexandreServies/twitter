package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CommunityResponse(
        String id,
        String name,
        String description,
        String question,
        @JsonProperty("member_count") Integer memberCount,
        @JsonProperty("moderator_count") Integer moderatorCount,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("join_policy") String joinPolicy,
        @JsonProperty("invites_policy") String invitesPolicy,
        @JsonProperty("is_nsfw") Boolean isNsfw,
        @JsonProperty("is_pinned") Boolean isPinned,
        String role,
        @JsonProperty("primary_topic") CommunityTopic primaryTopic,
        @JsonProperty("banner_url") String bannerUrl,
        @JsonProperty("search_tags") List<String> searchTags,
        List<CommunityRule> rules,
        CommunityMember creator,
        CommunityMember admin,
        @JsonProperty("members_preview") List<CommunityMember> membersPreview
) {}
