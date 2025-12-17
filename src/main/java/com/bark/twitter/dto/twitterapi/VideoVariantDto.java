package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoVariantDto(
        int bitrate,
        String content_type,
        String url
) {
    public static VideoVariantDto fromUrl(String url) {
        String contentType = "video/mp4";
        if (url.endsWith(".m3u8") || url.contains(".m3u8")) {
            contentType = "application/x-mpegURL";
        }
        return new VideoVariantDto(0, contentType, url);
    }
}
