package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionCreatorDto(
        String name,
        String screenName,
        String description,
        String profileBannerUrl,
        String profileImageUrlHttps,
        boolean isBlueVerified,
        @JsonInclude(JsonInclude.Include.ALWAYS) String verifiedType,
        int followersCount,
        int followingCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String screenName;
        private String description = "";
        private String profileBannerUrl = "";
        private String profileImageUrlHttps;
        private boolean isBlueVerified;
        private String verifiedType = "";
        private int followersCount;
        private int followingCount;

        public Builder name(String name) { this.name = name; return this; }
        public Builder screenName(String screenName) { this.screenName = screenName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder profileBannerUrl(String profileBannerUrl) { this.profileBannerUrl = profileBannerUrl; return this; }
        public Builder profileImageUrlHttps(String profileImageUrlHttps) { this.profileImageUrlHttps = profileImageUrlHttps; return this; }
        public Builder isBlueVerified(boolean isBlueVerified) { this.isBlueVerified = isBlueVerified; return this; }
        public Builder verifiedType(String verifiedType) { this.verifiedType = verifiedType; return this; }
        public Builder followersCount(int followersCount) { this.followersCount = followersCount; return this; }
        public Builder followingCount(int followingCount) { this.followingCount = followingCount; return this; }

        public AxionCreatorDto build() {
            return new AxionCreatorDto(
                    name, screenName, description, profileBannerUrl, profileImageUrlHttps,
                    isBlueVerified, verifiedType, followersCount, followingCount
            );
        }
    }
}
