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

import static com.bark.twitter.util.TwitterMediaProxy.proxyVideoUrl;

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

        // Extract following_count and affiliate_badge from user_profile
        int following = 0;
        AxionBadgeInfoDto badgeInfo = null;
        JsonNode userProfile = synopticTweet.get("user_profile");
        if (userProfile != null && !userProfile.isNull()) {
            following = getInt(userProfile, "following_count");
            badgeInfo = mapBadgeInfo(userProfile.get("affiliate_badge"));
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
                .following(following)
                .createdAt(createdAt)
                .isAutomated(false) // Not available in Synoptic
                .bioDescription(description)
                .badgeInfo(badgeInfo)
                .build();
    }

    /**
     * Maps affiliate badge info from Synoptic data.
     */
    private AxionBadgeInfoDto mapBadgeInfo(JsonNode badgeNode) {
        if (badgeNode == null || badgeNode.isNull()) {
            return null;
        }

        String badgeImageUrl = getText(badgeNode, "badge_url");
        String badgeDescription = getText(badgeNode, "description");
        String badgeUrl = getText(badgeNode, "link_url");

        // Only return badge info if we have at least the image URL
        if (badgeImageUrl.isEmpty()) {
            return null;
        }

        return new AxionBadgeInfoDto(badgeImageUrl, badgeDescription, badgeUrl);
    }

    /**
     * Maps entities from a Synoptic tweet.
     */
    private AxionEntitiesDto mapEntities(JsonNode synopticTweet) {
        String text = getText(synopticTweet, "text");
        List<AxionUrlEntityDto> urls = new ArrayList<>();
        List<AxionHashtagDto> hashtags = new ArrayList<>();
        List<AxionUserMentionDto> userMentions = new ArrayList<>();
        List<AxionSymbolDto> symbols = new ArrayList<>();

        // Extract all t.co URLs from text in order
        List<TcoUrlMatch> tcoMatches = extractTcoUrls(text);

        // Map URLs from Synoptic's urls array, matching with t.co URLs by position
        if (synopticTweet.has("urls") && synopticTweet.get("urls").isArray()) {
            int urlIndex = 0;
            for (JsonNode urlNode : synopticTweet.get("urls")) {
                String expandedUrl = urlNode.asText();
                if (expandedUrl != null && !expandedUrl.isEmpty()) {
                    // Match with t.co URL by position (assumes same order)
                    TcoUrlMatch tcoMatch = urlIndex < tcoMatches.size() ? tcoMatches.get(urlIndex) : null;
                    urls.add(createUrlEntity(expandedUrl, tcoMatch));
                    urlIndex++;
                }
            }
        }

        // Map hashtags from Synoptic's hashtags array
        if (synopticTweet.has("hashtags") && synopticTweet.get("hashtags").isArray()) {
            for (JsonNode hashtagNode : synopticTweet.get("hashtags")) {
                String tag = hashtagNode.asText();
                if (tag != null && !tag.isEmpty()) {
                    List<Integer> indices = findIndicesInText(text, "#" + tag);
                    hashtags.add(new AxionHashtagDto(indices, tag));
                }
            }
        }

        // Map mentions from Synoptic's mentions array
        if (synopticTweet.has("mentions") && synopticTweet.get("mentions").isArray()) {
            for (JsonNode mentionNode : synopticTweet.get("mentions")) {
                String screenName = mentionNode.asText();
                if (screenName != null && !screenName.isEmpty()) {
                    List<Integer> indices = findIndicesInText(text, "@" + screenName);
                    userMentions.add(new AxionUserMentionDto("", indices, "", screenName));
                }
            }
        }

        // Extract symbols/cashtags by parsing the tweet text
        symbols = extractSymbolsFromText(text);

        return AxionEntitiesDto.builder()
                .hashtags(hashtags)
                .urls(urls)
                .userMentions(userMentions)
                .symbols(symbols)
                .build();
    }

    private record TcoUrlMatch(String url, int start, int end) {}

    /**
     * Extracts all t.co URLs from text with their positions.
     */
    private List<TcoUrlMatch> extractTcoUrls(String text) {
        List<TcoUrlMatch> matches = new ArrayList<>();
        if (text == null) return matches;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https://t\\.co/[a-zA-Z0-9]+");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            matches.add(new TcoUrlMatch(matcher.group(), matcher.start(), matcher.end()));
        }
        return matches;
    }

    /**
     * Creates a URL entity from expanded URL and optional t.co match.
     */
    private AxionUrlEntityDto createUrlEntity(String expandedUrl, TcoUrlMatch tcoMatch) {
        String displayUrl = createDisplayUrl(expandedUrl);

        if (tcoMatch != null) {
            List<Integer> indices = List.of(tcoMatch.start(), tcoMatch.end());
            return new AxionUrlEntityDto(displayUrl, expandedUrl, indices, tcoMatch.url());
        }

        return new AxionUrlEntityDto(displayUrl, expandedUrl, List.of(), expandedUrl);
    }

    /**
     * Creates a shortened display URL from an expanded URL.
     */
    private String createDisplayUrl(String expandedUrl) {
        String display = expandedUrl;
        if (display.startsWith("https://")) {
            display = display.substring(8);
        } else if (display.startsWith("http://")) {
            display = display.substring(7);
        }
        if (display.startsWith("www.")) {
            display = display.substring(4);
        }
        // Truncate long URLs
        if (display.length() > 30) {
            display = display.substring(0, 27) + "…";
        }
        return display;
    }

    /**
     * Finds the start and end indices of a substring in text.
     */
    private List<Integer> findIndicesInText(String text, String substring) {
        if (text == null || substring == null) {
            return List.of();
        }
        int start = text.indexOf(substring);
        if (start >= 0) {
            return List.of(start, start + substring.length());
        }
        return List.of();
    }

    /**
     * Extracts cashtag symbols ($WORD) from tweet text.
     */
    private List<AxionSymbolDto> extractSymbolsFromText(String text) {
        List<AxionSymbolDto> symbols = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return symbols;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$([a-zA-Z][a-zA-Z0-9]*)");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String symbolText = matcher.group(1);
            List<Integer> indices = List.of(matcher.start(), matcher.end());
            symbols.add(new AxionSymbolDto(indices, symbolText));
        }

        return symbols;
    }

    /**
     * Maps extendedEntities (media) from a Synoptic tweet.
     */
    private AxionExtendedEntitiesDto mapExtendedEntities(JsonNode synopticTweet) {
        List<AxionMediaDto> mediaList = new ArrayList<>();

        if (!synopticTweet.has("media") || !synopticTweet.get("media").isArray()) {
            return null;
        }

        String text = getText(synopticTweet, "text");
        String screenName = getText(synopticTweet, "screen_name");
        String tweetId = getText(synopticTweet, "tweet_id");

        // Find all t.co URLs in text
        List<TcoUrlMatch> allTcoUrls = extractTcoUrls(text);

        // Get the t.co URLs that are used for link entities (from urls array)
        java.util.Set<String> linkTcoUrls = new java.util.HashSet<>();
        if (synopticTweet.has("urls") && synopticTweet.get("urls").isArray()) {
            for (int i = 0; i < synopticTweet.get("urls").size() && i < allTcoUrls.size(); i++) {
                linkTcoUrls.add(allTcoUrls.get(i).url());
            }
        }

        // Media t.co URLs are the ones NOT used for links
        List<TcoUrlMatch> mediaTcoUrls = allTcoUrls.stream()
                .filter(tco -> !linkTcoUrls.contains(tco.url()))
                .toList();

        int mediaIndex = 0;
        for (JsonNode mediaNode : synopticTweet.get("media")) {
            TcoUrlMatch tcoMatch = mediaIndex < mediaTcoUrls.size() ? mediaTcoUrls.get(mediaIndex) : null;
            AxionMediaDto media = mapMedia(mediaNode, screenName, tweetId, mediaIndex + 1, tcoMatch);
            if (media != null) {
                mediaList.add(media);
            }
            mediaIndex++;
        }

        return mediaList.isEmpty() ? null : AxionExtendedEntitiesDto.withMedia(mediaList);
    }

    /**
     * Maps a media object from Synoptic to Axion format.
     */
    private AxionMediaDto mapMedia(JsonNode mediaNode, String screenName, String tweetId,
                                    int mediaNumber, TcoUrlMatch tcoMatch) {
        String mediaUrl;
        String type;

        if (mediaNode.isTextual()) {
            mediaUrl = mediaNode.asText();
            type = inferMediaType(mediaUrl);
        } else {
            mediaUrl = getText(mediaNode, "url");
            type = getText(mediaNode, "type");
            if (type.isEmpty()) {
                type = inferMediaType(mediaUrl);
            }
        }

        if (mediaUrl.isEmpty()) {
            return null;
        }

        // Map type to Axion format
        String axionType = switch (type.toLowerCase()) {
            case "image" -> "photo";
            case "video" -> "video";
            case "gif", "animated_gif" -> "animated_gif";
            default -> "photo";
        };

        // Proxy video URLs for both mediaUrlHttps and videoUrl
        boolean isVideo = "video".equals(axionType) || "animated_gif".equals(axionType);
        String mediaUrlForDto = isVideo ? proxyVideoUrl(mediaUrl) : mediaUrl;

        AxionMediaDto.Builder builder = AxionMediaDto.builder()
                .type(axionType)
                .mediaUrlHttps(mediaUrlForDto)
                .extMediaAvailability(AxionMediaAvailabilityDto.available());

        // Populate fields derivable from t.co URL match
        if (tcoMatch != null) {
            String tcoCode = tcoMatch.url().replace("https://t.co/", "");
            builder.url(tcoMatch.url())
                    .displayUrl("pic.twitter.com/" + tcoCode)
                    .indices(List.of(tcoMatch.start(), tcoMatch.end()));
        }

        // Construct expanded URL
        if (!screenName.isEmpty() && !tweetId.isEmpty()) {
            String mediaType = "video".equals(axionType) || "animated_gif".equals(axionType) ? "video" : "photo";
            builder.expandedUrl("https://twitter.com/" + screenName + "/status/" + tweetId + "/" + mediaType + "/" + mediaNumber);
        }

        if (isVideo) {
            builder.videoUrl(mediaUrlForDto);
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

        // Map affiliate badge
        AxionBadgeInfoDto badgeInfo = mapBadgeInfo(synopticUser.get("affiliate_badge"));

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
                .badgeInfo(badgeInfo)
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
