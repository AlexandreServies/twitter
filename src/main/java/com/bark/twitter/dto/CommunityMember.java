package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommunityMember(
        String id,
        String name,
        @JsonProperty("screen_name") String screenName,
        String location,
        String url,
        String description,
        String email,
        @JsonProperty("protected") Boolean isProtected,
        Boolean verified,
        @JsonProperty("followers_count") Integer followersCount,
        @JsonProperty("following_count") Integer followingCount,
        @JsonProperty("friends_count") Integer friendsCount,
        @JsonProperty("favourites_count") Integer favouritesCount,
        @JsonProperty("statuses_count") Integer statusesCount,
        @JsonProperty("media_tweets_count") Integer mediaTweetsCount,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("profile_banner_url") String profileBannerUrl,
        @JsonProperty("profile_image_url_https") String profileImageUrlHttps,
        @JsonProperty("can_dm") Boolean canDm,
        Boolean isBlueVerified
) {}
