package com.mediasequencer.schedule;

import java.time.Instant;

public record ScheduleSyncEvent(long mediaId, Instant startAt, int durationSeconds) {
}
