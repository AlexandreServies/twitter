package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileBioDto(
        String description,
        Map<String, Object> entities,
        List<String> withheld_in_countries
) {
    public static ProfileBioDto from(String description) {
        return new ProfileBioDto(
                description,
                Map.of("description", Map.of()),
                List.of()
        );
    }
}
