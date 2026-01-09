package com.bark.twitter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for community data returned by the /communities batch endpoint.
 */
@Schema(description = "Community data with member count")
public record CommunityDataDto(
        @Schema(description = "Number of members in the community", example = "50000")
        long memberCount
) {}
