package com.bark.twitter.dto.axion;

public record AxionMediaAvailabilityDto(
        String status
) {
    public static AxionMediaAvailabilityDto available() {
        return new AxionMediaAvailabilityDto("Available");
    }
}
