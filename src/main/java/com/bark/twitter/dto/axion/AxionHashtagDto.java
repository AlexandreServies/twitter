package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Hashtag entity in tweet")
public record AxionHashtagDto(
        @Schema(description = "Character indices in tweet text") List<Integer> indices,
        @Schema(description = "Hashtag text without #", example = "crypto") String text
) {
}
