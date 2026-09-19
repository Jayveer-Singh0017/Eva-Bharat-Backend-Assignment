package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetCycleRequest(
        @JsonProperty("cycle_seconds") Integer cycleSeconds
) {
}
