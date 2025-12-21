package com.bark.twitter.mapper;

import com.bark.twitter.dto.axion.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mapper that transforms Synoptic API responses to Axion format.
 */
@Component
public class SynopticToAxiomMapper {

    private static final DateTimeFormatter TWITTER_DATE_FORMAT = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    /**
     * Converts a Synoptic tweet JsonNode to an AxionTweetDto.
     */
    public AxionTweetDto mapTweet(JsonNode synopticTweet) {
        String tweetId = getText(synopticTweet, "tweet_id");
        String screenName = getText(synopticTweet, "screen_name");
        String tweetType = getText(synopticTweet, "tweet_type");
        String text = getText(synopticTweet, "text");

        boolean isReply = "REPLY".equals(tweetType);
        boolean isQuote = "QUOTE".equals(tweetType);
        boolean isRetweet = "RETWEET".equals(tweetType);

        String url = "https://x.com/" + screenName + "/status/" + tweetId;

        // Parse timestamp
        long publishedAt = synopticTweet.has("published_at") ? synopticTweet.get("published_at").asLong() : 0;
        String createdAt = formatTwitterDate(publishedAt);

        // Build userInfo
        AxionUserInfoDto userInfo = mapUserInfo(synopticTweet);

        // Build entities
        AxionEntitiesDto entities = mapEntities(synopticTweet);

        // Build extendedEntities (media)
        AxionExtendedEntitiesDto extendedEntities = mapExtendedEntities(synopticTweet);

        // Handle reply info
        String inReplyToId = "";
        String inReplyToUserId = "";
        String inReplyToUsername = "";
        String conversationId = tweetId;
        AxionTweetDto replyTweet = null;

        if (isReply) {
            JsonNode reply = synopticTweet.get("reply");
            if (reply != null && !reply.isNull()) {
                if (reply.has("tweet_id")) {
                    // Enriched reply data - full tweet object
                    inReplyToId = getText(reply, "tweet_id");
                    inReplyToUsername = getText(reply, "screen_name");
                    inReplyToUserId = getText(reply, "user_id");
                    replyTweet = mapTweet(reply);
                } else {
                    // Original Synoptic reply format
                    inReplyToId = getText(reply, "reply_to_status_id");
                    inReplyToUsername = getText(reply, "reply_to_screen_name");
                }
                if (!inReplyToId.isEmpty()) {
                    conversationId = inReplyToId;
                }
            }
        }

        // Handle nested tweets (quote, retweet)
        AxionTweetDto quotedTweet = null;
        AxionTweetDto retweetedTweet = null;

        if (isQuote && synopticTweet.has("quote") && !synopticTweet.get("quote").isNull()) {
            quotedTweet = mapTweet(synopticTweet.get("quote"));
        }

        if (isRetweet && synopticTweet.has("retweet") && !synopticTweet.get("retweet").isNull()) {
            retweetedTweet = mapTweet(synopticTweet.get("retweet"));
        }

        return AxionTweetDto.builder()
                .userInfo(userInfo)
                .type("tweet")
                .id(tweetId)
                .url(url)
                .text(text)
                .lang(getText(synopticTweet, "lang"))
                .retweetCount(getInt(synopticTweet, "retweet_count"))
                .replyCount(getInt(synopticTweet, "reply_count"))
                .likeCount(getInt(synopticTweet, "like_count"))
                .quoteCount(getInt(synopticTweet, "quote_count"))
                .viewCount(getLong(synopticTweet, "view_count"))
                .createdAt(createdAt)
                .bookmarkCount(0) // Not available in Synoptic
                .isReply(isReply)
                .inReplyToId(inReplyToId)
                .conversationId(conversationId)
                .inReplyToUserId(inReplyToUserId)
                .inReplyToUsername(inReplyToUsername)
                .isPinned(false) // Not available in Synoptic
                .isRetweet(isRetweet)
                .isQuote(isQuote)
                .extendedEntities(extendedEntities)
                .entities(entities)
                .quotedTweet(quotedTweet)
                .retweetedTweet(retweetedTweet)
                .replyTweet(replyTweet)
                .build();
    }

    /**
     * Maps the userInfo from a Synoptic tweet.
     */
    private AxionUserInfoDto mapUserInfo(JsonNode synopticTweet) {
        String screenName = getText(synopticTweet, "screen_name");
        String description = getText(synopticTweet, "bio");

        // Extract user_extended_info fields
        JsonNode extendedInfo = synopticTweet.get("user_extended_info");
        String location = "";
        String coverImage = "";
        String createdAt = "";
        if (extendedInfo != null && !extendedInfo.isNull()) {
            location = getText(extendedInfo, "location");
            coverImage = getText(extendedInfo, "profile_banner_url");
            createdAt = getText(extendedInfo, "created_at"); // Keep Twitter format for tweet's userInfo
        }

        return AxionUserInfoDto.builder()
                .userName(screenName)
                .name(getText(synopticTweet, "name"))
                .isBlueVerified(getBool(synopticTweet, "is_blue_verified"))
                .verifiedType(getTextOrNull(synopticTweet, "verified_type"))
                .profilePicture(getText(synopticTweet, "logo"))
                .coverImage(coverImage)
                .description(description)
                .location(location)
                .followers(getInt(synopticTweet, "followers_count"))
                .following(0) // Not available in Synoptic tweet data
                .createdAt(createdAt)
                .isAutomated(false) // Not available in Synoptic
                .bioDescription(description)
                .badgeInfo(null) // TODO: Not available in Synoptic
                .build();
    }

    /**
     * Maps entities from a Synoptic tweet.
     */
    private AxionEntitiesDto mapEntities(JsonNode synopticTweet) {
        List<AxionUrlEntityDto> urls = new ArrayList<>();
        List<AxionHashtagDto> hashtags = new ArrayList<>();
        List<AxionUserMentionDto> userMentions = new ArrayList<>();

        // Map URLs from Synoptic's urls array
        if (synopticTweet.has("urls") && synopticTweet.get("urls").isArray()) {
            for (JsonNode urlNode : synopticTweet.get("urls")) {
                String expandedUrl = urlNode.asText();
                if (expandedUrl != null && !expandedUrl.isEmpty()) {
                    urls.add(AxionUrlEntityDto.from(expandedUrl));
                }
            }
        }

        // Map hashtags from Synoptic's hashtags array
        if (synopticTweet.has("hashtags") && synopticTweet.get("hashtags").isArray()) {
            for (JsonNode hashtagNode : synopticTweet.get("hashtags")) {
                String tag = hashtagNode.asText();
                if (tag != null && !tag.isEmpty()) {
                    hashtags.add(new AxionHashtagDto(List.of(), tag));
                }
            }
        }

        // Map mentions from Synoptic's mentions array
        if (synopticTweet.has("mentions") && synopticTweet.get("mentions").isArray()) {
            for (JsonNode mentionNode : synopticTweet.get("mentions")) {
                String screenName = mentionNode.asText();
                if (screenName != null && !screenName.isEmpty()) {
                    userMentions.add(new AxionUserMentionDto("", List.of(), "", screenName));
                }
            }
        }

        return AxionEntitiesDto.builder()
                .hashtags(hashtags)
                .urls(urls)
                .userMentions(userMentions)
                .symbols(List.of())
                .build();
    }

    /**
     * Maps extendedEntities (media) from a Synoptic tweet.
     */
    private AxionExtendedEntitiesDto mapExtendedEntities(JsonNode synopticTweet) {
        List<AxionMediaDto> mediaList = new ArrayList<>();

        if (synopticTweet.has("media") && synopticTweet.get("media").isArray()) {
            for (JsonNode mediaNode : synopticTweet.get("media")) {
                AxionMediaDto media = mapMedia(mediaNode);
                if (media != null) {
                    mediaList.add(media);
                }
            }
        }

        return mediaList.isEmpty() ? null : AxionExtendedEntitiesDto.withMedia(mediaList);
    }

    /**
     * Maps a media object from Synoptic to Axion format.
     */
    private AxionMediaDto mapMedia(JsonNode mediaNode) {
        String url;
        String type;

        if (mediaNode.isTextual()) {
            url = mediaNode.asText();
            type = inferMediaType(url);
        } else {
            url = getText(mediaNode, "url");
            type = getText(mediaNode, "type");
            if (type.isEmpty()) {
                type = inferMediaType(url);
            }
        }

        if (url.isEmpty()) {
            return null;
        }

        // Map type
        String axionType = switch (type.toLowerCase()) {
            case "image" -> "photo";
            case "video" -> "video";
            case "gif", "animated_gif" -> "video";
            default -> "photo";
        };

        AxionMediaDto.Builder builder = AxionMediaDto.builder()
                .type(axionType)
                .mediaUrlHttps(url)
                .extMediaAvailability(AxionMediaAvailabilityDto.available())
                .originalInfo(AxionOriginalInfoDto.empty());

        if ("video".equals(axionType)) {
            builder.videoUrl(url);
        }

        return builder.build();
    }

    private String inferMediaType(String url) {
        if (url.contains(".mp4") || url.contains("/vid/")) {
            return "video";
        } else if (url.contains(".gif")) {
            return "video";
        }
        return "photo";
    }

    /**
     * Formats a Unix timestamp (milliseconds) to Twitter's date format.
     */
    private String formatTwitterDate(long timestampMs) {
        if (timestampMs <= 0) {
            return "";
        }
        return TWITTER_DATE_FORMAT.format(Instant.ofEpochMilli(timestampMs));
    }

    // Helper methods for safe JSON access

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

    private long getLong(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return 0;
        }
        return node.get(field).asLong(0);
    }

    private boolean getBool(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return false;
        }
        return node.get(field).asBoolean(false);
    }

    /**
     * Maps a Synoptic user JsonNode to an AxionUserInfoDto (for /user endpoint).
     */
    public AxionUserInfoDto mapUser(JsonNode synopticUser) {
        String screenName = getText(synopticUser, "screen_name");
        String description = getText(synopticUser, "bio");
        if (description.isEmpty()) {
            description = getText(synopticUser, "description");
        }

        // Get profile image URL - try multiple field names
        String profilePicture = getText(synopticUser, "profile_image_url");
        if (profilePicture.isEmpty()) {
            profilePicture = getText(synopticUser, "logo");
        }

        // Convert createdAt from Twitter format to ISO format
        String createdAt = convertTwitterDateToIso(getText(synopticUser, "created_at"));

        return AxionUserInfoDto.builder()
                .userName(screenName)
                .name(getText(synopticUser, "name"))
                .isBlueVerified(getBool(synopticUser, "is_blue_verified"))
                .verifiedType(getTextOrNull(synopticUser, "verified_type"))
                .profilePicture(profilePicture)
                .coverImage(getText(synopticUser, "profile_banner_url"))
                .description(description)
                .location(getText(synopticUser, "location"))
                .followers(getInt(synopticUser, "followers_count"))
                .following(getInt(synopticUser, "following_count"))
                .createdAt(createdAt)
                .isAutomated(false) // Not available in Synoptic
                .bioDescription("") // Empty as per Axion examples
                .badgeInfo(null) // TODO: Not available in Synoptic
                .build();
    }

    /**
     * Converts Twitter date format "Sat Apr 20 04:51:51 +0000 2024" to ISO format "2024-04-20T04:51:51.000000Z".
     */
    private String convertTwitterDateToIso(String twitterDate) {
        if (twitterDate == null || twitterDate.isEmpty()) {
            return "";
        }
        try {
            java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter
                    .ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(twitterDate, inputFormatter);
            return zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
        } catch (Exception e) {
            return twitterDate; // Return original if parsing fails
        }
    }
}
