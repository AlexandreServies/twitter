package com.bark.twitter.dto.axion;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Twitter community information in Axiom format")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AxionCommunityDto(
        @Schema(description = "Community name", example = "Tech Enthusiasts") String name,
        @Schema(description = "Community description") String description,
        @Schema(description = "Number of community members") int memberCount,
        @Schema(description = "Creation timestamp", example = "Wed Jan 18 08:54:32 +0000 2023") String createdAt,
        @Schema(description = "Primary topic/category of the community") AxionPrimaryTopicDto primaryTopic,
        @Schema(description = "Community banner image URL") String bannerUrl,
        @Schema(description = "Community creator information") AxionCreatorDto creator,
        @Schema(description = "Preview of community members") List<AxionMemberPreviewDto> membersPreview
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
