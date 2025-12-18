package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionExtendedEntitiesDto(
        List<AxionMediaDto> media
) {
    public static AxionExtendedEntitiesDto empty() {
        return new AxionExtendedEntitiesDto(null);
    }

    public static AxionExtendedEntitiesDto withMedia(List<AxionMediaDto> media) {
        return new AxionExtendedEntitiesDto(media);
    }
}
