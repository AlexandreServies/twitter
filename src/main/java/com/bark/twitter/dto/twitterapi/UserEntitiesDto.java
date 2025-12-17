package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserEntitiesDto(
        Map<String, Object> description,
        UserUrlEntitiesDto url
) {
    public static UserEntitiesDto empty() {
        return new UserEntitiesDto(Map.of(), null);
    }

    public static UserEntitiesDto withUrl(List<UrlEntityDto> urls) {
        return new UserEntitiesDto(Map.of(), new UserUrlEntitiesDto(urls));
    }
}
