package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AxionCommunityDto(
        String name,
        String description,
        int memberCount,
        String createdAt,
        AxionPrimaryTopicDto primaryTopic,
        String bannerUrl,
        AxionCreatorDto creator,
        List<AxionMemberPreviewDto> membersPreview
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description = "";
        private int memberCount;
        private String createdAt;
        private AxionPrimaryTopicDto primaryTopic;
        private String bannerUrl = "";
        private AxionCreatorDto creator;
        private List<AxionMemberPreviewDto> membersPreview = List.of();

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder memberCount(int memberCount) { this.memberCount = memberCount; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder primaryTopic(AxionPrimaryTopicDto primaryTopic) { this.primaryTopic = primaryTopic; return this; }
        public Builder bannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; return this; }
        public Builder creator(AxionCreatorDto creator) { this.creator = creator; return this; }
        public Builder membersPreview(List<AxionMemberPreviewDto> membersPreview) { this.membersPreview = membersPreview; return this; }

        public AxionCommunityDto build() {
            return new AxionCommunityDto(
                    name, description, memberCount, createdAt, primaryTopic,
                    bannerUrl, creator, membersPreview
            );
        }
    }
}
