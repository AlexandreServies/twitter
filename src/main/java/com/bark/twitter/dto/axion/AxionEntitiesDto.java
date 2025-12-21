package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Tweet entities containing hashtags, URLs, mentions, and symbols")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionEntitiesDto(
        @Schema(description = "Hashtags in the tweet") List<AxionHashtagDto> hashtags,
        @Schema(description = "URLs in the tweet") List<AxionUrlEntityDto> urls,
        @Schema(description = "User mentions in the tweet") List<AxionUserMentionDto> userMentions,
        @Schema(description = "Cashtag symbols in the tweet") List<AxionSymbolDto> symbols
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
