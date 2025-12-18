package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionUserInfoDto(
        String userName,
        String name,
        boolean isBlueVerified,
        @JsonInclude(JsonInclude.Include.ALWAYS) String verifiedType,
        String profilePicture,
        String coverImage,
        String description,
        String location,
        int followers,
        int following,
        String createdAt,
        boolean isAutomated,
        String bioDescription,
        AxionBadgeInfoDto badgeInfo
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userName;
        private String name;
        private boolean isBlueVerified;
        private String verifiedType;
        private String profilePicture;
        private String coverImage = "";
        private String description = "";
        private String location = "";
        private int followers;
        private int following;
        private String createdAt = "";
        private boolean isAutomated;
        private String bioDescription = "";
        private AxionBadgeInfoDto badgeInfo;

        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder isBlueVerified(boolean isBlueVerified) { this.isBlueVerified = isBlueVerified; return this; }
        public Builder verifiedType(String verifiedType) { this.verifiedType = verifiedType; return this; }
        public Builder profilePicture(String profilePicture) { this.profilePicture = profilePicture; return this; }
        public Builder coverImage(String coverImage) { this.coverImage = coverImage; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder followers(int followers) { this.followers = followers; return this; }
        public Builder following(int following) { this.following = following; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder isAutomated(boolean isAutomated) { this.isAutomated = isAutomated; return this; }
        public Builder bioDescription(String bioDescription) { this.bioDescription = bioDescription; return this; }
        public Builder badgeInfo(AxionBadgeInfoDto badgeInfo) { this.badgeInfo = badgeInfo; return this; }

        public AxionUserInfoDto build() {
            return new AxionUserInfoDto(
                    userName, name, isBlueVerified, verifiedType, profilePicture,
                    coverImage, description, location, followers, following,
                    createdAt, isAutomated, bioDescription, badgeInfo
            );
        }
    }
}
