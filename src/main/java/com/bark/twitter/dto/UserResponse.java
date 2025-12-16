package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
        @JsonProperty("user_id") String userId,
        @JsonProperty("user_url") String userUrl,
        @JsonProperty("screen_name") String screenName,
        String name,
        @JsonProperty("followers_count") Integer followersCount,
        @JsonProperty("following_count") Integer followingCount,
        @JsonProperty("statuses_count") Integer statusesCount,
        @JsonProperty("created_at") String createdAt,
        String description,
        @JsonProperty("profile_image_url") String profileImageUrl,
        @JsonProperty("verified_type") String verifiedType,
        @JsonProperty("is_blue_verified") Boolean isBlueVerified,
        String location
) {}
