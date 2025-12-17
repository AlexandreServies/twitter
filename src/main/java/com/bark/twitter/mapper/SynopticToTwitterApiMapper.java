package com.bark.twitter.mapper;

import com.bark.twitter.dto.twitterapi.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mapper that transforms Synoptic API responses to TwitterAPI format.
 */
@Component
public class SynopticToTwitterApiMapper {

    private static final DateTimeFormatter TWITTER_DATE_FORMAT = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    /**
     * Converts a Synoptic tweet JsonNode to a TweetDto.
     */
    public TweetDto mapTweet(JsonNode synopticTweet) {
        return mapTweet(synopticTweet, null);
    }

    /**
     * Converts a Synoptic tweet JsonNode to a TweetDto with optional reply tweet data.
     */
    public TweetDto mapTweet(JsonNode synopticTweet, JsonNode repliedToTweet) {
        String tweetId = getText(synopticTweet, "tweet_id");
        String screenName = getText(synopticTweet, "screen_name");
        String tweetType = getText(synopticTweet, "tweet_type");
        String text = getText(synopticTweet, "text");

        boolean isReply = "REPLY".equals(tweetType);
        boolean isQuote = "QUOTE".equals(tweetType);
        boolean isRetweet = "RETWEET".equals(tweetType);

        // Build URLs
        String url = "https://x.com/" + screenName + "/status/" + tweetId;
        String twitterUrl = "https://twitter.com/" + screenName + "/status/" + tweetId;

        // Parse timestamp
        long publishedAt = synopticTweet.has("published_at") ? synopticTweet.get("published_at").asLong() : 0;
        String createdAt = formatTwitterDate(publishedAt);

        // Build author
        AuthorDto author = mapAuthor(synopticTweet);

        // Build entities
        EntitiesDto entities = mapEntities(synopticTweet, text, isReply);

        // Build extended entities (media)
        ExtendedEntitiesDto extendedEntities = mapExtendedEntities(synopticTweet);

        // Handle reply info
        String inReplyToId = "";
        String inReplyToUsername = "";
        String conversationId = tweetId; // TODO: Not available in Synoptic, default to tweet ID

        if (isReply) {
            JsonNode reply = synopticTweet.get("reply");
            if (reply != null && !reply.isNull()) {
                // If reply is a full tweet object (enriched), extract the ID
                if (reply.has("tweet_id")) {
                    inReplyToId = getText(reply, "tweet_id");
                    inReplyToUsername = getText(reply, "screen_name");
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
        TweetDto quotedTweet = null;
        TweetDto retweetedTweet = null;

        if (isQuote && synopticTweet.has("quote") && !synopticTweet.get("quote").isNull()) {
            quotedTweet = mapTweet(synopticTweet.get("quote"), null);
        }

        if (isRetweet && synopticTweet.has("retweet") && !synopticTweet.get("retweet").isNull()) {
            retweetedTweet = mapTweet(synopticTweet.get("retweet"), null);
        }

        // Calculate displayTextRange - TODO: Not available in Synoptic
        List<Integer> displayTextRange = List.of(0, text.length());

        return TweetDto.builder()
                .type("tweet")
                .id(tweetId)
                .url(url)
                .twitterUrl(twitterUrl)
                .text(text)
                .source("") // TODO: Not available in Synoptic
                .retweetCount(getInt(synopticTweet, "retweet_count"))
                .replyCount(getInt(synopticTweet, "reply_count"))
                .likeCount(getInt(synopticTweet, "like_count"))
                .quoteCount(getInt(synopticTweet, "quote_count"))
                .viewCount(getLong(synopticTweet, "view_count"))
                .createdAt(createdAt)
                .lang(getText(synopticTweet, "lang"))
                .bookmarkCount(0) // TODO: Not available in Synoptic
                .isReply(isReply)
                .inReplyToId(inReplyToId)
                .conversationId(conversationId)
                .displayTextRange(displayTextRange)
                .inReplyToUserId("") // TODO: Not available in Synoptic
                .inReplyToUsername(inReplyToUsername)
                .isPinned(false) // TODO: Not available in Synoptic
                .author(author)
                .extendedEntities(extendedEntities)
                .card(Map.of()) // TODO: Not available in Synoptic
                .place(Map.of()) // TODO: Not available in Synoptic
                .entities(entities)
                .isRetweet(isRetweet)
                .isQuote(isQuote)
                .isConversationControlled(false) // TODO: Not available in Synoptic
                .quotedTweet(quotedTweet)
                .retweetedTweet(retweetedTweet)
                .isLimitedReply(false) // TODO: Not available in Synoptic
                .build();
    }

    /**
     * Maps the author data from a Synoptic tweet (limited fields available).
     */
    public AuthorDto mapAuthor(JsonNode synopticTweet) {
        String screenName = getText(synopticTweet, "screen_name");
        String description = getText(synopticTweet, "bio");

        return AuthorDto.builder()
                .type("user")
                .id(getText(synopticTweet, "user_id"))
                .name(getText(synopticTweet, "name"))
                .userName(screenName)
                .location("") // TODO: Not available in Synoptic tweet data
                .description(description)
                .isProtected(false) // TODO: Not available in Synoptic
                .isVerified(false) // TODO: Legacy verification not available
                .isBlueVerified(getBool(synopticTweet, "is_blue_verified"))
                .verifiedType(getTextOrNull(synopticTweet, "verified_type"))
                .followers(getInt(synopticTweet, "followers_count"))
                .following(0) // Not available in Synoptic tweet data
                .favouritesCount(0) // Not available in Synoptic
                .statusesCount(getInt(synopticTweet, "statuses_count"))
                .mediaCount(0) // Not available in Synoptic
                .createdAt("") // Not available in Synoptic tweet data
                .profilePicture(getText(synopticTweet, "logo"))
                .canDm(false) // Not available in Synoptic
                .affiliatesHighlightedLabel(Map.of())
                .isAutomated(false) // Not available in Synoptic
                .pinnedTweetIds(List.of()) // Not available in Synoptic
                .twitterUrl("https://twitter.com/" + screenName)
                .profileBio(ProfileBioDto.from(description))
                .build();
    }

    /**
     * Maps a Synoptic user JsonNode to an AuthorDto (for /user endpoint).
     */
    public AuthorDto mapUser(JsonNode synopticUser) {
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

        return AuthorDto.builder()
                .id(getText(synopticUser, "user_id"))
                .name(getText(synopticUser, "name"))
                .userName(screenName)
                .location(getText(synopticUser, "location"))
                .url(null) // TODO: Not available in Synoptic
                .description(description)
                .entities(UserEntitiesDto.empty())
                .isProtected(false) // TODO: Not available in Synoptic
                .isVerified(false) // TODO: Legacy verification not available in Synoptic
                .isBlueVerified(getBool(synopticUser, "is_blue_verified"))
                .verifiedType(getTextOrNull(synopticUser, "verified_type"))
                .followers(getInt(synopticUser, "followers_count"))
                .following(getInt(synopticUser, "following_count"))
                .favouritesCount(0) // TODO: Not available in Synoptic
                .statusesCount(getInt(synopticUser, "statuses_count"))
                .mediaCount(0) // TODO: Not available in Synoptic
                .createdAt(createdAt)
                .coverPicture(null) // TODO: Not available in Synoptic
                .profilePicture(profilePicture)
                .canDm(false) // TODO: Not available in Synoptic
                .affiliatesHighlightedLabel(Map.of())
                .isAutomated(false) // TODO: Not available in Synoptic
                .automatedBy(null)
                .pinnedTweetIds(List.of()) // TODO: Not available in Synoptic
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

    /**
     * Maps entities (urls, user_mentions) from a Synoptic tweet.
     */
    private EntitiesDto mapEntities(JsonNode synopticTweet, String text, boolean isReply) {
        List<UrlEntityDto> urls = new ArrayList<>();
        List<UserMentionDto> mentions = new ArrayList<>();

        // Map URLs from Synoptic's urls array
        if (synopticTweet.has("urls") && synopticTweet.get("urls").isArray()) {
            for (JsonNode urlNode : synopticTweet.get("urls")) {
                String expandedUrl = urlNode.asText();
                if (expandedUrl != null && !expandedUrl.isEmpty()) {
                    urls.add(UrlEntityDto.from(expandedUrl));
                }
            }
        }

        // Extract mentions from reply data - TODO: Full mention data not available in Synoptic
        if (isReply) {
            JsonNode reply = synopticTweet.get("reply");
            if (reply != null && !reply.isNull()) {
                String replyToScreenName = reply.has("screen_name")
                        ? getText(reply, "screen_name")
                        : getText(reply, "reply_to_screen_name");
                if (!replyToScreenName.isEmpty()) {
                    String mentionPattern = "@" + replyToScreenName;
                    int idx = text.indexOf(mentionPattern);
                    if (idx >= 0) {
                        mentions.add(UserMentionDto.from(
                                replyToScreenName,
                                "", // TODO: User ID not available
                                "", // TODO: Display name not available
                                idx,
                                idx + mentionPattern.length()
                        ));
                    }
                }
            }
        }

        return new EntitiesDto(
                urls.isEmpty() ? null : urls,
                mentions.isEmpty() ? null : mentions
        );
    }

    /**
     * Maps extended entities (media) from a Synoptic tweet.
     */
    private ExtendedEntitiesDto mapExtendedEntities(JsonNode synopticTweet) {
        List<MediaDto> mediaList = new ArrayList<>();

        // Use media array (already transformed from mediaV2 by service)
        if (synopticTweet.has("media") && synopticTweet.get("media").isArray()) {
            for (JsonNode mediaNode : synopticTweet.get("media")) {
                MediaDto media = mapMedia(mediaNode);
                if (media != null) {
                    mediaList.add(media);
                }
            }
        }

        return mediaList.isEmpty() ? ExtendedEntitiesDto.empty() : ExtendedEntitiesDto.withMedia(mediaList);
    }

    /**
     * Maps a media object from Synoptic.
     */
    private MediaDto mapMedia(JsonNode mediaNode) {
        // Handle both object and string formats
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

        // Map Synoptic type to Twitter type
        String twitterType = switch (type.toLowerCase()) {
            case "image" -> "photo";
            case "video" -> "video";
            case "gif", "animated_gif" -> "animated_gif";
            default -> "photo";
        };

        MediaDto.Builder builder = MediaDto.builder()
                .type(twitterType)
                .mediaUrlHttps(url);

        if ("video".equals(twitterType) || "animated_gif".equals(twitterType)) {
            builder.videoInfo(VideoInfoDto.withSingleVariant(url));
        }

        return builder.build();
    }

    private String inferMediaType(String url) {
        if (url.contains(".mp4") || url.contains("/vid/")) {
            return "video";
        } else if (url.contains(".gif")) {
            return "animated_gif";
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
}
