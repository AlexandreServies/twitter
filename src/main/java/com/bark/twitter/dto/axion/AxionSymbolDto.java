package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cashtag symbol entity in tweet")
public record AxionSymbolDto(
        @Schema(description = "Character indices in tweet text") List<Integer> indices,
        @Schema(description = "Symbol text without $", example = "BTC") String text
) {
}
