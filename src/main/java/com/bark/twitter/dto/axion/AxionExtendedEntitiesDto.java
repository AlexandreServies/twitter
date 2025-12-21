package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Extended tweet entities containing media attachments")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionExtendedEntitiesDto(
        @Schema(description = "Media attachments (images, videos, GIFs)") List<AxionMediaDto> media
) {
    public static AxionExtendedEntitiesDto empty() {
        return new AxionExtendedEntitiesDto(null);
    }

    public static AxionExtendedEntitiesDto withMedia(List<AxionMediaDto> media) {
        return new AxionExtendedEntitiesDto(media);
    }
}
