package com.bark.twitter.dto.axion;

import java.util.List;

public record AxionSymbolDto(
        List<Integer> indices,
        String text
) {
}
