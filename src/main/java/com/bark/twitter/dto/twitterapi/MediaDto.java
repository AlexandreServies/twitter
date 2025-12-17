package com.bark.twitter.dto.twitterapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaDto(
        String display_url,
        String expanded_url,
        String id_str,
        List<Integer> indices,
        String media_key,
        String media_url_https,
        Map<String, Object> original_info,
        Map<String, Object> sizes,
        String type,
        String url,
        VideoInfoDto video_info
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String display_url = "";
        private String expanded_url = "";
        private String id_str = "";
        private List<Integer> indices = List.of();
        private String media_key = "";
        private String media_url_https;
        private Map<String, Object> original_info = Map.of();
        private Map<String, Object> sizes = Map.of();
        private String type;
        private String url = "";
        private VideoInfoDto video_info;

        public Builder displayUrl(String display_url) { this.display_url = display_url; return this; }
        public Builder expandedUrl(String expanded_url) { this.expanded_url = expanded_url; return this; }
        public Builder idStr(String id_str) { this.id_str = id_str; return this; }
        public Builder indices(List<Integer> indices) { this.indices = indices; return this; }
        public Builder mediaKey(String media_key) { this.media_key = media_key; return this; }
        public Builder mediaUrlHttps(String media_url_https) { this.media_url_https = media_url_https; return this; }
        public Builder originalInfo(Map<String, Object> original_info) { this.original_info = original_info; return this; }
        public Builder sizes(Map<String, Object> sizes) { this.sizes = sizes; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder videoInfo(VideoInfoDto video_info) { this.video_info = video_info; return this; }

        public MediaDto build() {
            return new MediaDto(
                    display_url, expanded_url, id_str, indices,
                    media_key, media_url_https, original_info, sizes,
                    type, url, video_info
            );
        }
    }
}
