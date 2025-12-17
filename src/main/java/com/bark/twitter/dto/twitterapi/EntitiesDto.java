package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitiesDto(
        List<UrlEntityDto> urls,
        List<UserMentionDto> user_mentions
) {
    public static EntitiesDto empty() {
        return new EntitiesDto(List.of(), List.of());
    }
}
