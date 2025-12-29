package com.bark.twitter.mapper;

import com.bark.twitter.dto.axion.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.bark.twitter.util.TwitterMediaProxy.proxyVideoUrl;

/**
 * Mapper that transforms TwitterAPI.io responses to Axion format.
 * Handles tweets and users.
 */
@Component
public class TwitterApiToAxionMapper {

    /**
     * Converts a TwitterAPI.io tweet JsonNode to an AxionTweetDto.
     */
    public AxionTweetDto mapTweet(JsonNode twitterApiTweet) {
        String tweetId = getText(twitterApiTweet, "id");
        String url = getText(twitterApiTweet, "url");
        String text = getText(twitterApiTweet, "text");

        boolean isReply = getBool(twitterApiTweet, "isReply");
        boolean isQuote = getBool(twitterApiTweet, "isQuote");
        boolean isRetweet = getBool(twitterApiTweet, "isRetweet");

        // Build userInfo from author
        AxionUserInfoDto userInfo = mapAuthorToUserInfo(twitterApiTweet.get("author"));

        // Build entities
        AxionEntitiesDto entities = mapEntities(twitterApiTweet.get("entities"), text);

        // Build extendedEntities (media)
        AxionExtendedEntitiesDto extendedEntities = mapExtendedEntities(twitterApiTweet.get("extendedEntities"));

        // Handle nested tweets
        AxionTweetDto quotedTweet = null;
        AxionTweetDto retweetedTweet = null;

        if (isQuote && twitterApiTweet.has("quoted_tweet") && !twitterApiTweet.get("quoted_tweet").isNull()) {
            quotedTweet = mapTweet(twitterApiTweet.get("quoted_tweet"));
        }

        if (isRetweet && twitterApiTweet.has("retweeted_tweet") && !twitterApiTweet.get("retweeted_tweet").isNull()) {
            retweetedTweet = mapTweet(twitterApiTweet.get("retweeted_tweet"));
        }

        return AxionTweetDto.builder()
                .userInfo(userInfo)
                .type("tweet")
                .id(tweetId)
                .url(url)
                .text(text)
                .lang(getText(twitterApiTweet, "lang"))
                .retweetCount(getInt(twitterApiTweet, "retweetCount"))
                .replyCount(getInt(twitterApiTweet, "replyCount"))
                .likeCount(getInt(twitterApiTweet, "likeCount"))
                .quoteCount(getInt(twitterApiTweet, "quoteCount"))
                .viewCount(getLong(twitterApiTweet, "viewCount"))
                .createdAt(getText(twitterApiTweet, "createdAt"))
                .bookmarkCount(getInt(twitterApiTweet, "bookmarkCount"))
                .isReply(isReply)
                .inReplyToId(getText(twitterApiTweet, "inReplyToId"))
                .conversationId(getText(twitterApiTweet, "conversationId"))
                .inReplyToUserId(getText(twitterApiTweet, "inReplyToUserId"))
                .inReplyToUsername(getText(twitterApiTweet, "inReplyToUsername"))
                .isPinned(getBool(twitterApiTweet, "isPinned"))
                .isRetweet(isRetweet)
                .isQuote(isQuote)
                .extendedEntities(extendedEntities)
                .entities(entities)
                .quotedTweet(quotedTweet)
                .retweetedTweet(retweetedTweet)
                .replyTweet(null) // TwitterAPI.io doesn't include full reply tweet data inline
                .build();
    }

    /**
     * Maps the author from a TwitterAPI.io tweet to AxionUserInfoDto.
     */
    private AxionUserInfoDto mapAuthorToUserInfo(JsonNode author) {
        if (author == null || author.isNull()) {
            return null;
        }

        return AxionUserInfoDto.builder()
                .userName(getText(author, "userName"))
                .name(getText(author, "name"))
                .isBlueVerified(getBool(author, "isBlueVerified"))
                .verifiedType(getTextOrNull(author, "verifiedType"))
                .profilePicture(getText(author, "profilePicture"))
                .coverImage(getText(author, "coverPicture"))
                .description(getText(author, "description"))
                .location(getText(author, "location"))
                .followers(getInt(author, "followers"))
                .following(getInt(author, "following"))
                .createdAt(getText(author, "createdAt"))
                .isAutomated(getBool(author, "isAutomated"))
                .bioDescription(getText(author, "description"))
                .badgeInfo(mapBadgeInfo(author.get("affiliatesHighlightedLabel")))
                .build();
    }

    /**
     * Maps a TwitterAPI.io user response to AxionUserInfoDto (for /user endpoint).
     */
    public AxionUserInfoDto mapUser(JsonNode twitterApiUser) {
        return AxionUserInfoDto.builder()
                .userName(getText(twitterApiUser, "userName"))
                .name(getText(twitterApiUser, "name"))
                .isBlueVerified(getBool(twitterApiUser, "isBlueVerified"))
                .verifiedType(getTextOrNull(twitterApiUser, "verifiedType"))
                .profilePicture(getText(twitterApiUser, "profilePicture"))
                .coverImage(getText(twitterApiUser, "coverPicture"))
                .description(getText(twitterApiUser, "description"))
                .location(getText(twitterApiUser, "location"))
                .followers(getInt(twitterApiUser, "followers"))
                .following(getInt(twitterApiUser, "following"))
                .createdAt(getText(twitterApiUser, "createdAt"))
                .isAutomated(getBool(twitterApiUser, "isAutomated"))
                .bioDescription("") // Empty as per Axion examples
                .badgeInfo(mapBadgeInfo(twitterApiUser.get("affiliatesHighlightedLabel")))
                .build();
    }

    /**
     * Maps affiliate badge info from TwitterAPI.io data.
     */
    private AxionBadgeInfoDto mapBadgeInfo(JsonNode badgeNode) {
        if (badgeNode == null || badgeNode.isNull() || badgeNode.isEmpty()) {
            return null;
        }

        // TwitterAPI.io uses affiliatesHighlightedLabel which may have different structure
        // Try to extract badge info if available
        JsonNode labelNode = badgeNode.get("label");
        if (labelNode == null || labelNode.isNull()) {
            return null;
        }

        JsonNode badgeInfo = labelNode.get("badge");
        if (badgeInfo == null || badgeInfo.isNull()) {
            return null;
        }

        String badgeImageUrl = getText(badgeInfo, "url");
        if (badgeImageUrl.isEmpty()) {
            return null;
        }

        String badgeDescription = getText(labelNode, "description");
        String badgeUrl = getText(labelNode, "url");

        return new AxionBadgeInfoDto(badgeImageUrl, badgeDescription, badgeUrl);
    }

    /**
     * Maps entities from a TwitterAPI.io tweet.
     */
    private AxionEntitiesDto mapEntities(JsonNode entitiesNode, String text) {
        if (entitiesNode == null || entitiesNode.isNull()) {
            return AxionEntitiesDto.empty();
        }

        List<AxionHashtagDto> hashtags = new ArrayList<>();
        List<AxionUrlEntityDto> urls = new ArrayList<>();
        List<AxionUserMentionDto> userMentions = new ArrayList<>();
        List<AxionSymbolDto> symbols = new ArrayList<>();

        // Map hashtags
        if (entitiesNode.has("hashtags") && entitiesNode.get("hashtags").isArray()) {
            for (JsonNode hashtagNode : entitiesNode.get("hashtags")) {
                String tag = getText(hashtagNode, "text");
                List<Integer> indices = getIndices(hashtagNode);
                if (!tag.isEmpty()) {
                    hashtags.add(new AxionHashtagDto(indices, tag));
                }
            }
        }

        // Map URLs
        if (entitiesNode.has("urls") && entitiesNode.get("urls").isArray()) {
            for (JsonNode urlNode : entitiesNode.get("urls")) {
                String displayUrl = getText(urlNode, "display_url");
                String expandedUrl = getText(urlNode, "expanded_url");
                List<Integer> indices = getIndices(urlNode);
                String url = getText(urlNode, "url");

                if (!expandedUrl.isEmpty()) {
                    urls.add(new AxionUrlEntityDto(displayUrl, expandedUrl, indices, url));
                }
            }
        }

        // Map user mentions
        if (entitiesNode.has("user_mentions") && entitiesNode.get("user_mentions").isArray()) {
            for (JsonNode mentionNode : entitiesNode.get("user_mentions")) {
                String idStr = getText(mentionNode, "id_str");
                List<Integer> indices = getIndices(mentionNode);
                String name = getText(mentionNode, "name");
                String screenName = getText(mentionNode, "screen_name");
                userMentions.add(new AxionUserMentionDto(idStr, indices, name, screenName));
            }
        }

        // Map symbols/cashtags
        if (entitiesNode.has("symbols") && entitiesNode.get("symbols").isArray()) {
            for (JsonNode symbolNode : entitiesNode.get("symbols")) {
                String symbolText = getText(symbolNode, "text");
                List<Integer> indices = getIndices(symbolNode);
                if (!symbolText.isEmpty()) {
                    symbols.add(new AxionSymbolDto(indices, symbolText));
                }
            }
        }

        return AxionEntitiesDto.builder()
                .hashtags(hashtags)
                .urls(urls)
                .userMentions(userMentions)
                .symbols(symbols)
                .build();
    }

    /**
     * Maps extendedEntities (media) from a TwitterAPI.io tweet.
     */
    private AxionExtendedEntitiesDto mapExtendedEntities(JsonNode extendedEntitiesNode) {
        if (extendedEntitiesNode == null || extendedEntitiesNode.isNull()) {
            return null;
        }

        JsonNode mediaNode = extendedEntitiesNode.get("media");
        if (mediaNode == null || !mediaNode.isArray() || mediaNode.isEmpty()) {
            return null;
        }

        List<AxionMediaDto> mediaList = new ArrayList<>();

        for (JsonNode media : mediaNode) {
            AxionMediaDto mediaDto = mapMedia(media);
            if (mediaDto != null) {
                mediaList.add(mediaDto);
            }
        }

        return mediaList.isEmpty() ? null : AxionExtendedEntitiesDto.withMedia(mediaList);
    }

    /**
     * Maps a single media object from TwitterAPI.io to Axion format.
     */
    private AxionMediaDto mapMedia(JsonNode media) {
        String type = getText(media, "type");
        String mediaUrlHttps = getText(media, "media_url_https");

        if (mediaUrlHttps.isEmpty()) {
            return null;
        }

        boolean isVideo = "video".equals(type) || "animated_gif".equals(type);

        // Get video URL from video_info if available
        String videoUrl = null;
        if (isVideo) {
            JsonNode videoInfo = media.get("video_info");
            if (videoInfo != null && !videoInfo.isNull()) {
                JsonNode variants = videoInfo.get("variants");
                if (variants != null && variants.isArray()) {
                    // Find the best quality video (highest bitrate mp4)
                    int bestBitrate = -1;
                    for (JsonNode variant : variants) {
                        String contentType = getText(variant, "content_type");
                        if ("video/mp4".equals(contentType)) {
                            int bitrate = getInt(variant, "bitrate");
                            if (bitrate > bestBitrate) {
                                bestBitrate = bitrate;
                                videoUrl = getText(variant, "url");
                            }
                        }
                    }
                    // If no mp4 found, use first variant
                    if (videoUrl == null && !variants.isEmpty()) {
                        videoUrl = getText(variants.get(0), "url");
                    }
                }
            }

            // Proxy video URLs
            if (videoUrl != null) {
                videoUrl = proxyVideoUrl(videoUrl);
            }
            mediaUrlHttps = isVideo ? proxyVideoUrl(mediaUrlHttps) : mediaUrlHttps;
        }

        return AxionMediaDto.builder()
                .displayUrl(getText(media, "display_url"))
                .expandedUrl(getText(media, "expanded_url"))
                .extMediaAvailability(AxionMediaAvailabilityDto.available())
                .idStr(getText(media, "id_str"))
                .indices(getIndices(media))
                .mediaKey(getText(media, "media_key"))
                .mediaUrlHttps(mediaUrlHttps)
                .type(type)
                .url(getText(media, "url"))
                .videoUrl(videoUrl)
                .build();
    }

    /**
     * Gets indices array from a JSON node.
     */
    private List<Integer> getIndices(JsonNode node) {
        if (node == null || !node.has("indices") || !node.get("indices").isArray()) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>();
        for (JsonNode idx : node.get("indices")) {
            indices.add(idx.asInt());
        }
        return indices;
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
        String value = node.get(field).asText();
        return value.isEmpty() ? null : value;
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
