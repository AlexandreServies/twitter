package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the /follows endpoint.
 * Contains followers/following counts for found users, plus lists of not-found and errored usernames.
 */
public record FollowsResponseDto(
        Map<String, UserFollows> users,
        @JsonProperty("not_found") List<String> notFound,
        @JsonProperty("errors") List<String> errors
) {
    /**
     * Follower and following counts for a single user.
     */
    public record UserFollows(
            long followers,
            long following
    ) {}
}
