package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionEntitiesDto(
        List<AxionHashtagDto> hashtags,
        List<AxionUrlEntityDto> urls,
        List<AxionUserMentionDto> userMentions,
        List<AxionSymbolDto> symbols
) {
    public static AxionEntitiesDto empty() {
        return new AxionEntitiesDto(List.of(), List.of(), List.of(), List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<AxionHashtagDto> hashtags = List.of();
        private List<AxionUrlEntityDto> urls = List.of();
        private List<AxionUserMentionDto> userMentions = List.of();
        private List<AxionSymbolDto> symbols = List.of();

        public Builder hashtags(List<AxionHashtagDto> hashtags) { this.hashtags = hashtags; return this; }
        public Builder urls(List<AxionUrlEntityDto> urls) { this.urls = urls; return this; }
        public Builder userMentions(List<AxionUserMentionDto> userMentions) { this.userMentions = userMentions; return this; }
        public Builder symbols(List<AxionSymbolDto> symbols) { this.symbols = symbols; return this; }

        public AxionEntitiesDto build() {
            return new AxionEntitiesDto(hashtags, urls, userMentions, symbols);
        }
    }
}
