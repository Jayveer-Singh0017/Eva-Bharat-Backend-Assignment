package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WindowDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("items") List<WindowItemDto> items,
        @JsonProperty("resolved") ResolvedDto resolved
) {
}
