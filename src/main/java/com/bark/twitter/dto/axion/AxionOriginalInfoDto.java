package com.bark.twitter.dto.axion;

import java.util.List;
import java.util.Map;

public record AxionOriginalInfoDto(
        List<Map<String, Integer>> focusRects,
        int height,
        int width
) {
    public static AxionOriginalInfoDto empty() {
        return new AxionOriginalInfoDto(List.of(), 0, 0);
    }
}
