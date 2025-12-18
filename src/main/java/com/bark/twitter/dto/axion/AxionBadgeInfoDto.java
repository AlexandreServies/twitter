package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Badge info for Twitter affiliate/organization badges.
 * TODO: Not available in Synoptic - add support when data becomes available.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionBadgeInfoDto(
        String badgeImageUrl,
        String badgeDescription,
        String badgeUrl
) {
}
