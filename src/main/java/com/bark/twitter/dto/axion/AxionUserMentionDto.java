package com.bark.twitter.dto.axion;

import java.util.List;

public record AxionUserMentionDto(
        String idStr,
        List<Integer> indices,
        String name,
        String screenName
) {
}
