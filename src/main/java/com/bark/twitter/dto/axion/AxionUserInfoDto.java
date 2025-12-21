package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Twitter user information in Axiom format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionUserInfoDto(
        @Schema(description = "Username/handle without @", example = "elonmusk") String userName,
        @Schema(description = "Display name", example = "Elon Musk") String name,
        @Schema(description = "Whether user has Twitter Blue verification") boolean isBlueVerified,
        @Schema(description = "Type of verification (e.g., 'Business', 'Government', or empty)") @JsonInclude(JsonInclude.Include.ALWAYS) String verifiedType,
        @Schema(description = "Profile picture URL") String profilePicture,
        @Schema(description = "Cover/banner image URL") String coverImage,
        @Schema(description = "User bio description") String description,
        @Schema(description = "User location", example = "Austin, Texas") String location,
        @Schema(description = "Follower count") int followers,
        @Schema(description = "Following count") int following,
        @Schema(description = "Account creation timestamp", example = "Tue Jun 02 20:12:29 +0000 2009") String createdAt,
        @Schema(description = "Whether this is an automated account") boolean isAutomated,
        @Schema(description = "Bio description (same as description)") String bioDescription,
        @Schema(description = "Badge information for affiliate accounts") AxionBadgeInfoDto badgeInfo
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
