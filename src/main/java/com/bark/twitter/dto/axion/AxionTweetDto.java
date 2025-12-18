package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionTweetDto(
        AxionUserInfoDto userInfo,
        String type,
        String id,
        String url,
        String text,
        String lang,
        int retweetCount,
        int replyCount,
        int likeCount,
        int quoteCount,
        long viewCount,
        String createdAt,
        int bookmarkCount,
        boolean isReply,
        String inReplyToId,
        String conversationId,
        String inReplyToUserId,
        String inReplyToUsername,
        boolean isPinned,
        boolean isRetweet,
        boolean isQuote,
        AxionExtendedEntitiesDto extendedEntities,
        AxionEntitiesDto entities,
        AxionTweetDto quotedTweet,
        AxionTweetDto retweetedTweet,
        AxionTweetDto replyTweet
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
