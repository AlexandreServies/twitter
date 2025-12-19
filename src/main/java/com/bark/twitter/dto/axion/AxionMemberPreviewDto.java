package com.bark.twitter.dto.axion;

public record AxionMemberPreviewDto(
        String profileImageUrlHttps,
        boolean isBlueVerified,
        int followersCount
) {
}
