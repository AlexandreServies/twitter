package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the /follows endpoint.
 * Contains followers/following counts for found users, plus lists of not-found and errored usernames.
 */
@Schema(description = "Response containing follower/following counts for requested users")
public record FollowsResponseDto(
        @Schema(description = "Map of username to follower/following counts for successfully fetched users",
                example = "{\"elonmusk\": {\"followers\": 150000000, \"following\": 500}}")
        Map<String, UserFollows> users,

        @Schema(description = "List of usernames that were not found on Twitter",
                example = "[\"nonexistentuser123\"]")
        @JsonProperty("not_found") List<String> notFound,

        @Schema(description = "List of usernames that encountered errors during lookup",
                example = "[\"erroreduser\"]")
        @JsonProperty("errors") List<String> errors
) {
    /**
     * Follower and following counts for a single user.
     */
    @Schema(description = "Follower and following counts for a user")
    public record UserFollows(
            @Schema(description = "Number of followers", example = "150000000")
            long followers,
            @Schema(description = "Number of accounts the user is following", example = "500")
            long following
    ) {}
}
