package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Community creator information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionCreatorDto(
        @Schema(description = "Display name", example = "Community Admin") String name,
        @Schema(description = "Username/handle", example = "admin") String screenName,
        @Schema(description = "User bio description") String description,
        @Schema(description = "Profile banner image URL") String profileBannerUrl,
        @Schema(description = "Profile picture URL") String profileImageUrlHttps,
        @Schema(description = "Whether user has Twitter Blue verification") Boolean isBlueVerified,
        @Schema(description = "Type of verification") String verifiedType,
        @Schema(description = "Follower count") Integer followersCount,
        @Schema(description = "Following count") Integer followingCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static AxionCreatorDto empty() {
        return new AxionCreatorDto(null, null, null, null, null, null, null, null, null);
    }

    public static class Builder {
        private String name;
        private String screenName;
        private String description = "";
        private String profileBannerUrl = "";
        private String profileImageUrlHttps;
        private Boolean isBlueVerified;
        private String verifiedType = "";
        private Integer followersCount;
        private Integer followingCount;

        public Builder name(String name) { this.name = name; return this; }
        public Builder screenName(String screenName) { this.screenName = screenName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder profileBannerUrl(String profileBannerUrl) { this.profileBannerUrl = profileBannerUrl; return this; }
        public Builder profileImageUrlHttps(String profileImageUrlHttps) { this.profileImageUrlHttps = profileImageUrlHttps; return this; }
        public Builder isBlueVerified(Boolean isBlueVerified) { this.isBlueVerified = isBlueVerified; return this; }
        public Builder verifiedType(String verifiedType) { this.verifiedType = verifiedType; return this; }
        public Builder followersCount(Integer followersCount) { this.followersCount = followersCount; return this; }
        public Builder followingCount(Integer followingCount) { this.followingCount = followingCount; return this; }

        public AxionCreatorDto build() {
            return new AxionCreatorDto(
                    name, screenName, description, profileBannerUrl, profileImageUrlHttps,
                    isBlueVerified, verifiedType, followersCount, followingCount
            );
        }
    }
}
