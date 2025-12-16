package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TweetResponse(
        @JsonProperty("tweet_id") String tweetId,
        @JsonProperty("user_id") String userId,
        String logo,
        @JsonProperty("statuses_count") Integer statusesCount,
        String text,
        String lang,
        String name,
        @JsonProperty("screen_name") String screenName,
        @JsonProperty("user_url") String userUrl,
        @JsonProperty("tweet_url") String tweetUrl,
        @JsonProperty("published_at") Long publishedAt,
        @JsonProperty("followers_count") Integer followersCount,
        String bio,
        @JsonProperty("is_blue_verified") Boolean isBlueVerified,
        @JsonProperty("verified_type") String verifiedType,
        List<String> urls,
        List<String> media,
        @JsonProperty("mediaV2") List<MediaItem> mediaV2,
        @JsonProperty("view_count") Integer viewCount,
        @JsonProperty("like_count") Integer likeCount,
        @JsonProperty("retweet_count") Integer retweetCount,
        @JsonProperty("reply_count") Integer replyCount,
        @JsonProperty("quote_count") Integer quoteCount,
        @JsonProperty("tweet_type") String tweetType,
        TweetResponse retweet,
        TweetResponse quote,
        TweetResponse reply
) {}
