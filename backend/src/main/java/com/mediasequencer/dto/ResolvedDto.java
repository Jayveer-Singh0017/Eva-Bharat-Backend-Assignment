package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResolvedDto(
        @JsonProperty("media_id") long mediaId,
        @JsonProperty("kind") String kind,
        @JsonProperty("url") String url,
        @JsonProperty("remaining_seconds") double remainingSeconds,
        @JsonProperty("is_sync") boolean isSync,
        @JsonProperty("blank") boolean blank
) {
}
