package com.bark.twitter.dto.axion;

import java.util.List;

public record AxionUrlEntityDto(
        String displayUrl,
        String expandedUrl,
        List<Integer> indices,
        String url
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
