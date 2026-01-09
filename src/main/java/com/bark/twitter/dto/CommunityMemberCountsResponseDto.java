package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the /communities endpoint.
 * Contains community data for found communities, plus lists of not-found and errored community IDs.
 */
@Schema(description = "Response containing data for requested communities")
public record CommunityMemberCountsResponseDto(
        @Schema(description = "Map of community ID to community data for successfully fetched communities",
                example = "{\"1234567890\": {\"memberCount\": 50000}, \"9876543210\": {\"memberCount\": 12500}}")
        Map<String, CommunityDataDto> communities,

        @Schema(description = "List of community IDs that were not found",
                example = "[\"nonexistent123\"]")
        @JsonProperty("not_found") List<String> notFound,

        @Schema(description = "List of community IDs that encountered errors during lookup",
                example = "[\"errored456\"]")
        List<String> errors
) {}
