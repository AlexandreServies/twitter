package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Badge info for Twitter affiliate/organization badges.
 * TODO: Not available in Synoptic - add support when data becomes available.
 */
@Schema(description = "Badge information for affiliate/organization accounts")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionBadgeInfoDto(
        @Schema(description = "Badge image URL") String badgeImageUrl,
        @Schema(description = "Badge description text") String badgeDescription,
        @Schema(description = "Link associated with the badge") String badgeUrl
) {
}
