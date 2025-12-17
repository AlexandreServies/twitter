package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoInfoDto(
        List<Integer> aspect_ratio,
        int duration_millis,
        List<VideoVariantDto> variants
) {
    public static VideoInfoDto withSingleVariant(String url) {
        return new VideoInfoDto(
                List.of(16, 9),
                0,
                List.of(VideoVariantDto.fromUrl(url))
        );
    }
}
