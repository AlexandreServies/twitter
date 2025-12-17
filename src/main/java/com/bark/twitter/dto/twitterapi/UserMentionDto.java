package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserMentionDto(
        String id_str,
        List<Integer> indices,
        String name,
        String screen_name
) {
    public static UserMentionDto from(String screenName, String userId, String name, int startIndex, int endIndex) {
        return new UserMentionDto(userId, List.of(startIndex, endIndex), name, screenName);
    }
}
