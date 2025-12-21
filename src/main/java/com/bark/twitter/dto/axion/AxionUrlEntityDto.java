package com.bark.twitter.dto.axion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "URL entity in tweet")
public record AxionUrlEntityDto(
        @Schema(description = "Shortened display URL", example = "example.com/page...") String displayUrl,
        @Schema(description = "Full expanded URL", example = "https://example.com/page/123") String expandedUrl,
        @Schema(description = "Character indices in tweet text") List<Integer> indices,
        @Schema(description = "t.co shortened URL") String url
) {
    public static AxionUrlEntityDto from(String expandedUrl) {
        String displayUrl = expandedUrl;
        if (displayUrl.startsWith("https://")) {
            displayUrl = displayUrl.substring(8);
        } else if (displayUrl.startsWith("http://")) {
            displayUrl = displayUrl.substring(7);
        }
        if (displayUrl.startsWith("www.")) {
            displayUrl = displayUrl.substring(4);
        }
        // Truncate long URLs for display
        if (displayUrl.length() > 30) {
            displayUrl = displayUrl.substring(0, 27) + "...";
        }
        return new AxionUrlEntityDto(displayUrl, expandedUrl, List.of(), expandedUrl);
    }
}
