package com.mediasequencer.schedule;

import java.time.Duration;

public record ScheduleResolved(
        long mediaId,
        Duration remaining,
        boolean isSync,
        boolean blank
) {
}
