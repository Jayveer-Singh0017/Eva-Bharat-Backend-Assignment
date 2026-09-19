package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TimeResponse(@JsonProperty("server_time") String serverTime) {
}
