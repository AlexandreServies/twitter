package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Media size variant dimensions")
public record AxionSizeDto(
        @Schema(description = "Height in pixels") int h,
        @Schema(description = "Width in pixels") int w
) {
}
