package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorDto(
        String type,
        String id,
        String name,
        String userName,
        String location,
        String url,
        String description,
        UserEntitiesDto entities,
        boolean isProtected,
        boolean isVerified,
        boolean isBlueVerified,
        String verifiedType,
        int followers,
        int following,
        int favouritesCount,
        int statusesCount,
        int mediaCount,
        String createdAt,
        String coverPicture,
        String profilePicture,
        boolean canDm,
        Map<String, Object> affiliatesHighlightedLabel,
        boolean isAutomated,
        String automatedBy,
        List<String> pinnedTweetIds,
        // Additional fields for tweet author context
        String twitterUrl,
        ProfileBioDto profile_bio
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "user";
        private String id;
        private String name;
        private String userName;
        private String location = "";
        private String url;
        private String description = "";
        private UserEntitiesDto entities;
        private boolean isProtected = false;
        private boolean isVerified = false;
        private boolean isBlueVerified = false;
        private String verifiedType;
        private int followers = 0;
        private int following = 0;
        private int favouritesCount = 0;
        private int statusesCount = 0;
        private int mediaCount = 0;
        private String createdAt = "";
        private String coverPicture;
        private String profilePicture;
        private boolean canDm = false;
        private Map<String, Object> affiliatesHighlightedLabel = Map.of();
        private boolean isAutomated = false;
        private String automatedBy;
        private List<String> pinnedTweetIds = List.of();
        private String twitterUrl;
        private ProfileBioDto profile_bio;

        public Builder type(String type) { this.type = type; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder entities(UserEntitiesDto entities) { this.entities = entities; return this; }
        public Builder isProtected(boolean isProtected) { this.isProtected = isProtected; return this; }
        public Builder isVerified(boolean isVerified) { this.isVerified = isVerified; return this; }
        public Builder isBlueVerified(boolean isBlueVerified) { this.isBlueVerified = isBlueVerified; return this; }
        public Builder verifiedType(String verifiedType) { this.verifiedType = verifiedType; return this; }
        public Builder followers(int followers) { this.followers = followers; return this; }
        public Builder following(int following) { this.following = following; return this; }
        public Builder favouritesCount(int favouritesCount) { this.favouritesCount = favouritesCount; return this; }
        public Builder statusesCount(int statusesCount) { this.statusesCount = statusesCount; return this; }
        public Builder mediaCount(int mediaCount) { this.mediaCount = mediaCount; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder coverPicture(String coverPicture) { this.coverPicture = coverPicture; return this; }
        public Builder profilePicture(String profilePicture) { this.profilePicture = profilePicture; return this; }
        public Builder canDm(boolean canDm) { this.canDm = canDm; return this; }
        public Builder affiliatesHighlightedLabel(Map<String, Object> affiliatesHighlightedLabel) { this.affiliatesHighlightedLabel = affiliatesHighlightedLabel; return this; }
        public Builder isAutomated(boolean isAutomated) { this.isAutomated = isAutomated; return this; }
        public Builder automatedBy(String automatedBy) { this.automatedBy = automatedBy; return this; }
        public Builder pinnedTweetIds(List<String> pinnedTweetIds) { this.pinnedTweetIds = pinnedTweetIds; return this; }
        public Builder twitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; return this; }
        public Builder profileBio(ProfileBioDto profile_bio) { this.profile_bio = profile_bio; return this; }

        public AuthorDto build() {
            return new AuthorDto(
                    type, id, name, userName, location, url, description,
                    entities, isProtected, isVerified, isBlueVerified, verifiedType,
                    followers, following, favouritesCount, statusesCount, mediaCount,
                    createdAt, coverPicture, profilePicture, canDm,
                    affiliatesHighlightedLabel, isAutomated, automatedBy, pinnedTweetIds,
                    twitterUrl, profile_bio
            );
        }
    }
}
