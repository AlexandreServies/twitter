package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preview of a community member")
public record AxionMemberPreviewDto(
        @Schema(description = "Profile picture URL") String profileImageUrlHttps,
        @Schema(description = "Whether user has Twitter Blue verification") boolean isBlueVerified,
        @Schema(description = "Follower count") int followersCount
) {
}
