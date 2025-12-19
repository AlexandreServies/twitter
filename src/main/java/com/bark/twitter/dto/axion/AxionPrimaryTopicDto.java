package com.bark.twitter.dto.axion;

public record AxionPrimaryTopicDto(
        String name
) {
    public static AxionPrimaryTopicDto empty() {
        return new AxionPrimaryTopicDto("");
    }
}
