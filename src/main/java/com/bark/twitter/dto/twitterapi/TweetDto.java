package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TweetDto(
        String type,
        String id,
        String url,
        String twitterUrl,
        String text,
        String source,
        int retweetCount,
        int replyCount,
        int likeCount,
        int quoteCount,
        long viewCount,
        String createdAt,
        String lang,
        int bookmarkCount,
        boolean isReply,
        String inReplyToId,
        String conversationId,
        List<Integer> displayTextRange,
        String inReplyToUserId,
        String inReplyToUsername,
        boolean isPinned,
        AuthorDto author,
        ExtendedEntitiesDto extendedEntities,
        Map<String, Object> card,
        Map<String, Object> place,
        EntitiesDto entities,
        boolean isRetweet,
        boolean isQuote,
        boolean isConversationControlled,
        TweetDto quoted_tweet,
        TweetDto retweeted_tweet,
        boolean isLimitedReply
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "tweet";
        private String id;
        private String url;
        private String twitterUrl;
        private String text;
        private String source = "";
        private int retweetCount;
        private int replyCount;
        private int likeCount;
        private int quoteCount;
        private long viewCount;
        private String createdAt;
        private String lang;
        private int bookmarkCount = 0;
        private boolean isReply;
        private String inReplyToId = "";
        private String conversationId;
        private List<Integer> displayTextRange;
        private String inReplyToUserId = "";
        private String inReplyToUsername = "";
        private boolean isPinned = false;
        private AuthorDto author;
        private ExtendedEntitiesDto extendedEntities;
        private Map<String, Object> card = Map.of();
        private Map<String, Object> place = Map.of();
        private EntitiesDto entities;
        private boolean isRetweet;
        private boolean isQuote;
        private boolean isConversationControlled = false;
        private TweetDto quoted_tweet;
        private TweetDto retweeted_tweet;
        private boolean isLimitedReply = false;

        public Builder type(String type) { this.type = type; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder twitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder retweetCount(int retweetCount) { this.retweetCount = retweetCount; return this; }
        public Builder replyCount(int replyCount) { this.replyCount = replyCount; return this; }
        public Builder likeCount(int likeCount) { this.likeCount = likeCount; return this; }
        public Builder quoteCount(int quoteCount) { this.quoteCount = quoteCount; return this; }
        public Builder viewCount(long viewCount) { this.viewCount = viewCount; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder lang(String lang) { this.lang = lang; return this; }
        public Builder bookmarkCount(int bookmarkCount) { this.bookmarkCount = bookmarkCount; return this; }
        public Builder isReply(boolean isReply) { this.isReply = isReply; return this; }
        public Builder inReplyToId(String inReplyToId) { this.inReplyToId = inReplyToId; return this; }
        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder displayTextRange(List<Integer> displayTextRange) { this.displayTextRange = displayTextRange; return this; }
        public Builder inReplyToUserId(String inReplyToUserId) { this.inReplyToUserId = inReplyToUserId; return this; }
        public Builder inReplyToUsername(String inReplyToUsername) { this.inReplyToUsername = inReplyToUsername; return this; }
        public Builder isPinned(boolean isPinned) { this.isPinned = isPinned; return this; }
        public Builder author(AuthorDto author) { this.author = author; return this; }
        public Builder extendedEntities(ExtendedEntitiesDto extendedEntities) { this.extendedEntities = extendedEntities; return this; }
        public Builder card(Map<String, Object> card) { this.card = card; return this; }
        public Builder place(Map<String, Object> place) { this.place = place; return this; }
        public Builder entities(EntitiesDto entities) { this.entities = entities; return this; }
        public Builder isRetweet(boolean isRetweet) { this.isRetweet = isRetweet; return this; }
        public Builder isQuote(boolean isQuote) { this.isQuote = isQuote; return this; }
        public Builder isConversationControlled(boolean isConversationControlled) { this.isConversationControlled = isConversationControlled; return this; }
        public Builder quotedTweet(TweetDto quoted_tweet) { this.quoted_tweet = quoted_tweet; return this; }
        public Builder retweetedTweet(TweetDto retweeted_tweet) { this.retweeted_tweet = retweeted_tweet; return this; }
        public Builder isLimitedReply(boolean isLimitedReply) { this.isLimitedReply = isLimitedReply; return this; }

        public TweetDto build() {
            return new TweetDto(
                    type, id, url, twitterUrl, text, source,
                    retweetCount, replyCount, likeCount, quoteCount, viewCount,
                    createdAt, lang, bookmarkCount, isReply, inReplyToId,
                    conversationId, displayTextRange, inReplyToUserId, inReplyToUsername,
                    isPinned, author, extendedEntities, card, place, entities,
                    isRetweet, isQuote, isConversationControlled, quoted_tweet,
                    retweeted_tweet, isLimitedReply
            );
        }
    }
}
