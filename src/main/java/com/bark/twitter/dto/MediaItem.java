package com.bark.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaItem(
        String type,
        String url,
        String caption,
        String credit
) {}
