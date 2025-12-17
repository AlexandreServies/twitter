package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UrlEntityDto(
        String display_url,
        String expanded_url,
        List<Integer> indices,
        String url
) {
    public static UrlEntityDto from(String expandedUrl) {
        String displayUrl = expandedUrl
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "");
        if (displayUrl.length() > 30) {
            displayUrl = displayUrl.substring(0, 30) + "...";
        }
        return new UrlEntityDto(displayUrl, expandedUrl, List.of(), "");
    }
}
