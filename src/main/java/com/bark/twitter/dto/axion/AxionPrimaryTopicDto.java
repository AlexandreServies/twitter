package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Primary topic/category of a community")
public record AxionPrimaryTopicDto(
        @Schema(description = "Topic name", example = "Technology") String name
) {
    public static AxionPrimaryTopicDto empty() {
        return new AxionPrimaryTopicDto("");
    }
}
