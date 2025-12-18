package com.bark.twitter.dto.axion;

import java.util.List;

public record AxionHashtagDto(
        List<Integer> indices,
        String text
) {
}
