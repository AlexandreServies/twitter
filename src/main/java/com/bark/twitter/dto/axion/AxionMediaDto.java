package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Media attachment (image, video, or GIF)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionMediaDto(
        @Schema(description = "Shortened display URL") String displayUrl,
        @Schema(description = "Full expanded URL") String expandedUrl,
        @Schema(description = "Media availability status") AxionMediaAvailabilityDto extMediaAvailability,
        @Schema(description = "Media features (face detection, etc.)") Map<String, Object> features,
        @Schema(description = "Media ID string") String idStr,
        @Schema(description = "Character indices in tweet text") List<Integer> indices,
        @Schema(description = "Media key identifier") String mediaKey,
        @Schema(description = "Direct media URL (HTTPS)") String mediaUrlHttps,
        @Schema(description = "Original media dimensions") AxionOriginalInfoDto originalInfo,
        @Schema(description = "Available size variants (thumb, small, medium, large)") Map<String, AxionSizeDto> sizes,
        @Schema(description = "Media type (photo, video, animated_gif)", example = "photo") String type,
        @Schema(description = "t.co shortened URL") String url,
        @Schema(description = "Direct video URL (for video/GIF types)") String videoUrl
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String displayUrl = "";
        private String expandedUrl = "";
        private AxionMediaAvailabilityDto extMediaAvailability;
        private Map<String, Object> features;
        private String idStr = "";
        private List<Integer> indices = List.of();
        private String mediaKey = "";
        private String mediaUrlHttps;
        private AxionOriginalInfoDto originalInfo;
        private Map<String, AxionSizeDto> sizes = Map.of();
        private String type;
        private String url = "";
        private String videoUrl;

        public Builder displayUrl(String displayUrl) { this.displayUrl = displayUrl; return this; }
        public Builder expandedUrl(String expandedUrl) { this.expandedUrl = expandedUrl; return this; }
        public Builder extMediaAvailability(AxionMediaAvailabilityDto extMediaAvailability) { this.extMediaAvailability = extMediaAvailability; return this; }
        public Builder features(Map<String, Object> features) { this.features = features; return this; }
        public Builder idStr(String idStr) { this.idStr = idStr; return this; }
        public Builder indices(List<Integer> indices) { this.indices = indices; return this; }
        public Builder mediaKey(String mediaKey) { this.mediaKey = mediaKey; return this; }
        public Builder mediaUrlHttps(String mediaUrlHttps) { this.mediaUrlHttps = mediaUrlHttps; return this; }
        public Builder originalInfo(AxionOriginalInfoDto originalInfo) { this.originalInfo = originalInfo; return this; }
        public Builder sizes(Map<String, AxionSizeDto> sizes) { this.sizes = sizes; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }

        public AxionMediaDto build() {
            return new AxionMediaDto(
                    displayUrl, expandedUrl, extMediaAvailability, features,
                    idStr, indices, mediaKey, mediaUrlHttps, originalInfo,
                    sizes, type, url, videoUrl
            );
        }
    }
}
