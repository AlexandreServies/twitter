package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tweet data in Axiom format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionTweetDto(
        @Schema(description = "Author information") AxionUserInfoDto userInfo,
        @Schema(description = "Content type", example = "tweet") String type,
        @Schema(description = "Tweet ID", example = "1234567890") String id,
        @Schema(description = "Tweet URL", example = "https://x.com/user/status/1234567890") String url,
        @Schema(description = "Tweet text content") String text,
        @Schema(description = "Language code", example = "en") String lang,
        @Schema(description = "Retweet count") int retweetCount,
        @Schema(description = "Reply count") int replyCount,
        @Schema(description = "Like count") int likeCount,
        @Schema(description = "Quote count") int quoteCount,
        @Schema(description = "View count") long viewCount,
        @Schema(description = "Creation timestamp", example = "Sat Dec 20 23:59:00 +0000 2025") String createdAt,
        @Schema(description = "Bookmark count") int bookmarkCount,
        @Schema(description = "Whether this is a reply") boolean isReply,
        @Schema(description = "ID of tweet being replied to") String inReplyToId,
        @Schema(description = "Conversation thread ID") String conversationId,
        @Schema(description = "User ID being replied to") String inReplyToUserId,
        @Schema(description = "Username being replied to") String inReplyToUsername,
        @Schema(description = "Whether tweet is pinned") boolean isPinned,
        @Schema(description = "Whether this is a retweet") boolean isRetweet,
        @Schema(description = "Whether this is a quote tweet") boolean isQuote,
        @Schema(description = "Media attachments") AxionExtendedEntitiesDto extendedEntities,
        @Schema(description = "Tweet entities (URLs, hashtags, mentions)") AxionEntitiesDto entities,
        @Schema(description = "Quoted tweet data") AxionTweetDto quotedTweet,
        @Schema(description = "Retweeted tweet data") AxionTweetDto retweetedTweet,
        @Schema(description = "Tweet being replied to") AxionTweetDto replyTweet
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AxionUserInfoDto userInfo;
        private String type = "tweet";
        private String id;
        private String url;
        private String text;
        private String lang = "";
        private int retweetCount;
        private int replyCount;
        private int likeCount;
        private int quoteCount;
        private long viewCount;
        private String createdAt;
        private int bookmarkCount;
        private boolean isReply;
        private String inReplyToId = "";
        private String conversationId;
        private String inReplyToUserId = "";
        private String inReplyToUsername = "";
        private boolean isPinned;
        private boolean isRetweet;
        private boolean isQuote;
        private AxionExtendedEntitiesDto extendedEntities;
        private AxionEntitiesDto entities;
        private AxionTweetDto quotedTweet;
        private AxionTweetDto retweetedTweet;
        private AxionTweetDto replyTweet;

        public Builder userInfo(AxionUserInfoDto userInfo) { this.userInfo = userInfo; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder lang(String lang) { this.lang = lang; return this; }
        public Builder retweetCount(int retweetCount) { this.retweetCount = retweetCount; return this; }
        public Builder replyCount(int replyCount) { this.replyCount = replyCount; return this; }
        public Builder likeCount(int likeCount) { this.likeCount = likeCount; return this; }
        public Builder quoteCount(int quoteCount) { this.quoteCount = quoteCount; return this; }
        public Builder viewCount(long viewCount) { this.viewCount = viewCount; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder bookmarkCount(int bookmarkCount) { this.bookmarkCount = bookmarkCount; return this; }
        public Builder isReply(boolean isReply) { this.isReply = isReply; return this; }
        public Builder inReplyToId(String inReplyToId) { this.inReplyToId = inReplyToId; return this; }
        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder inReplyToUserId(String inReplyToUserId) { this.inReplyToUserId = inReplyToUserId; return this; }
        public Builder inReplyToUsername(String inReplyToUsername) { this.inReplyToUsername = inReplyToUsername; return this; }
        public Builder isPinned(boolean isPinned) { this.isPinned = isPinned; return this; }
        public Builder isRetweet(boolean isRetweet) { this.isRetweet = isRetweet; return this; }
        public Builder isQuote(boolean isQuote) { this.isQuote = isQuote; return this; }
        public Builder extendedEntities(AxionExtendedEntitiesDto extendedEntities) { this.extendedEntities = extendedEntities; return this; }
        public Builder entities(AxionEntitiesDto entities) { this.entities = entities; return this; }
        public Builder quotedTweet(AxionTweetDto quotedTweet) { this.quotedTweet = quotedTweet; return this; }
        public Builder retweetedTweet(AxionTweetDto retweetedTweet) { this.retweetedTweet = retweetedTweet; return this; }
        public Builder replyTweet(AxionTweetDto replyTweet) { this.replyTweet = replyTweet; return this; }

        public AxionTweetDto build() {
            return new AxionTweetDto(
                    userInfo, type, id, url, text, lang,
                    retweetCount, replyCount, likeCount, quoteCount, viewCount,
                    createdAt, bookmarkCount, isReply, inReplyToId, conversationId,
                    inReplyToUserId, inReplyToUsername, isPinned, isRetweet, isQuote,
                    extendedEntities, entities, quotedTweet, retweetedTweet, replyTweet
            );
        }
    }
}
