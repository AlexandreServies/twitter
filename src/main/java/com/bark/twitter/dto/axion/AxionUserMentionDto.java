package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "User mention entity in tweet")
public record AxionUserMentionDto(
        @Schema(description = "User ID string") String idStr,
        @Schema(description = "Character indices in tweet text") List<Integer> indices,
        @Schema(description = "User display name") String name,
        @Schema(description = "Username/handle") String screenName
) {
}
