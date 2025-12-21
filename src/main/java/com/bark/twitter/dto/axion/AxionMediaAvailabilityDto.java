package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Media availability status")
public record AxionMediaAvailabilityDto(
        @Schema(description = "Availability status", example = "Available") String status
) {
    public static AxionMediaAvailabilityDto available() {
        return new AxionMediaAvailabilityDto("Available");
    }
}
