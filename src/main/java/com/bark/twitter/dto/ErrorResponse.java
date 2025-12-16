package com.bark.twitter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error message", example = "Tweet not found: 123456789")
        String error
) {}
