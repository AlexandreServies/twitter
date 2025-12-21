package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Original media dimensions and focus rectangles")
public record AxionOriginalInfoDto(
        @Schema(description = "Focus rectangles for cropping") List<Map<String, Integer>> focusRects,
        @Schema(description = "Original height in pixels") int height,
        @Schema(description = "Original width in pixels") int width
) {
    public static AxionOriginalInfoDto empty() {
        return new AxionOriginalInfoDto(List.of(), 0, 0);
    }
}
