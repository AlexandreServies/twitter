package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtendedEntitiesDto(
        List<MediaDto> media
) {
    public static ExtendedEntitiesDto empty() {
        return new ExtendedEntitiesDto(List.of());
    }

    public static ExtendedEntitiesDto withMedia(List<MediaDto> media) {
        return new ExtendedEntitiesDto(media);
    }
}
