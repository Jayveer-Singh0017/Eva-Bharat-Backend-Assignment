package com.mediasequencer.schedule;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Pure mathematical scheduling engine ported line-for-line from Go's backend/internal/schedule.
 *
 * What a window shows is a pure function of the current time, not stored state.
 * Nothing here is mutated and nothing here is stateful.
 */
@Service
public class ScheduleService {

    public static final Duration MIN_REMAINING = Duration.ofSeconds(1);

    /**
     * floorMod returns a mod n in the mathematical sense: always in [0, n),
     * never negative, for n > 0.
     */
    public static Duration floorMod(Duration a, Duration n) {
        long nNanos = n.toNanos();
        if (nNanos <= 0) {
            return Duration.ZERO;
        }
        long aNanos = a.toNanos();
        long m = aNanos % nNanos;
        if (m < 0) {
            m += nNanos;
        }
        return Duration.ofNanos(m);
    }

    /**
     * clampMin floors d to MIN_REMAINING.
     */
    public static Duration clampMin(Duration d) {
        if (d.compareTo(MIN_REMAINING) < 0) {
            return MIN_REMAINING;
        }
        return d;
    }

    /**
     * CurrentItem computes what a window's own playlist shows at now, with no
     * regard to any sync overlay.
     */
    public ScheduleResolved currentItem(Instant anchor, List<ScheduleItem> items, int cycleSeconds, Instant now) {
        Duration cycleDur = Duration.ofSeconds(cycleSeconds);
        if (cycleDur.isNegative() || cycleDur.isZero()) {
            return new ScheduleResolved(0L, MIN_REMAINING, false, true);
        }

        // Position within the current cycle. Always in [0, cycleDur).
        Duration elapsed = floorMod(Duration.between(anchor, now), cycleDur);
        // Time left before every window restarts its cycle together. Always in (0, cycleDur].
        Duration toCycleEnd = cycleDur.minus(elapsed);

        Duration playlistDur = Duration.ZERO;
        if (items != null) {
            for (ScheduleItem it : items) {
                if (it.durationSeconds() > 0) {
                    playlistDur = playlistDur.plusSeconds(it.durationSeconds());
                }
            }
        }

        if (playlistDur.isNegative() || playlistDur.isZero()) {
            // Empty playlist, or every item is invalid: nothing to show.
            return new ScheduleResolved(0L, clampMin(toCycleEnd), false, true);
        }

        // Position within the looping playlist.
        Duration offset = floorMod(elapsed, playlistDur);

        Duration acc = Duration.ZERO;
        for (ScheduleItem it : items) {
            if (it.durationSeconds() <= 0) {
                continue;
            }
            Duration d = Duration.ofSeconds(it.durationSeconds());
            if (offset.compareTo(acc.plus(d)) < 0) {
                Duration remaining = acc.plus(d).minus(offset);
                if (remaining.compareTo(toCycleEnd) > 0) {
                    remaining = toCycleEnd;
                }
                return new ScheduleResolved(it.mediaId(), clampMin(remaining), false, false);
            }
            acc = acc.plus(d);
        }

        return new ScheduleResolved(0L, clampMin(toCycleEnd), false, true);
    }

    /**
     * Resolve applies the sync overlay on top of CurrentItem. If sync is
     * active — start_at <= now < start_at+duration — every window shows its
     * media instead, with the remaining sync time. Otherwise each window
     * falls back to its own computed item.
     */
    public ScheduleResolved resolve(Instant anchor, List<ScheduleItem> items, int cycleSeconds, ScheduleSyncEvent sync, Instant now) {
        if (sync != null && sync.durationSeconds() > 0) {
            Instant start = sync.startAt();
            Instant end = start.plusSeconds(sync.durationSeconds());
            if (!now.isBefore(start) && now.isBefore(end)) {
                return new ScheduleResolved(
                        sync.mediaId(),
                        clampMin(Duration.between(now, end)),
                        true,
                        false
                );
            }
        }

        return currentItem(anchor, items, cycleSeconds, now);
    }
}
