package com.mediasequencer.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleServiceTest {

    private final ScheduleService service = new ScheduleService();
    private final Instant anchor = Instant.parse("2020-01-01T00:00:00Z");

    // standardItems: A(1, 10s), B(2, 20s), C(3, 15s) — playlist duration 45s.
    private final List<ScheduleItem> standardItems = List.of(
            new ScheduleItem(1, 10),
            new ScheduleItem(2, 20),
            new ScheduleItem(3, 15)
    );

    @Test
    @DisplayName("mid-item")
    void testMidItem() {
        Instant now = anchor.plusSeconds(25);
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(2);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("exact instant an item starts")
    void testExactInstantItemStarts() {
        Instant now = anchor.plusSeconds(10); // A ends / B starts
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(2);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(20));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("exact instant an item ends")
    void testExactInstantItemEnds() {
        Instant now = anchor.plusSeconds(30); // B ends / C starts
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(3);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(15));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("wrap at end of playlist")
    void testWrapAtEndOfPlaylist() {
        Instant now = anchor.plusSeconds(46); // 45 + 1: one second into a second lap
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(1);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(9));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("wrap at end of cycle (multiple cycles elapsed)")
    void testWrapAtEndOfCycle() {
        Instant now = anchor.plusSeconds(250); // 2 cycles + 50s
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(1);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("empty playlist")
    void testEmptyPlaylist() {
        Instant now = anchor.plusSeconds(30);
        ScheduleResolved resolved = service.currentItem(anchor, null, 100, now);

        assertThat(resolved.blank()).isTrue();
        assertThat(resolved.mediaId()).isEqualTo(0);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(70));
    }

    @Test
    @DisplayName("single item")
    void testSingleItem() {
        List<ScheduleItem> items = List.of(new ScheduleItem(7, 30));
        Instant now = anchor.plusSeconds(10);
        ScheduleResolved resolved = service.currentItem(anchor, items, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(7);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    @DisplayName("one item longer than the whole cycle")
    void testOneItemLongerThanWholeCycle() {
        List<ScheduleItem> items = List.of(new ScheduleItem(9, 500));
        Instant now = anchor.plusSeconds(40);
        ScheduleResolved resolved = service.currentItem(anchor, items, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(9);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("playlist doesn't divide the cycle evenly")
    void testPlaylistDoesNotDivideCycleEvenly() {
        Instant now = anchor.plusSeconds(93); // 97 = 2*45 + 7
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 97, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(1);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    @DisplayName("now exactly equal to the anchor")
    void testNowExactlyEqualToAnchor() {
        Instant now = anchor;
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(1);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("now before the anchor")
    void testNowBeforeAnchor() {
        Instant now = anchor.minusSeconds(10);
        ScheduleResolved resolved = service.currentItem(anchor, standardItems, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(1);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("zero-duration item mixed in is skipped")
    void testZeroDurationItemSkipped() {
        List<ScheduleItem> items = List.of(
                new ScheduleItem(1, 10),
                new ScheduleItem(99, 0),
                new ScheduleItem(3, 15)
        );
        Instant now = anchor.plusSeconds(12);
        ScheduleResolved resolved = service.currentItem(anchor, items, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(3);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(13));
    }

    @Test
    @DisplayName("remaining clamps to the minimum instead of a fraction of a second")
    void testRemainingClampsToMinimum() {
        List<ScheduleItem> items = List.of(new ScheduleItem(5, 100));
        Instant now = anchor.plusSeconds(99).plusMillis(500);
        ScheduleResolved resolved = service.currentItem(anchor, items, 100, now);

        assertThat(resolved.blank()).isFalse();
        assertThat(resolved.mediaId()).isEqualTo(5);
        assertThat(resolved.remaining()).isEqualTo(ScheduleService.MIN_REMAINING);
    }

    @Test
    @DisplayName("no sync falls back to the base schedule")
    void testResolveNoSync() {
        Instant now = anchor.plusSeconds(25);
        ScheduleResolved resolved = service.resolve(anchor, standardItems, 100, null, now);

        assertThat(resolved.mediaId()).isEqualTo(2);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("sync active")
    void testResolveSyncActive() {
        ScheduleSyncEvent sync = new ScheduleSyncEvent(42, anchor.plusSeconds(20), 10);
        Instant now = anchor.plusSeconds(25);
        ScheduleResolved resolved = service.resolve(anchor, standardItems, 100, sync, now);

        assertThat(resolved.mediaId()).isEqualTo(42);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isTrue();
    }

    @Test
    @DisplayName("sync just expired falls back to the base schedule")
    void testResolveSyncJustExpired() {
        ScheduleSyncEvent sync = new ScheduleSyncEvent(42, anchor.plusSeconds(20), 10);
        Instant now = anchor.plusSeconds(30);
        ScheduleResolved resolved = service.resolve(anchor, standardItems, 100, sync, now);

        assertThat(resolved.mediaId()).isEqualTo(3);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(15));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("sync in the future falls back to the base schedule")
    void testResolveSyncInFuture() {
        ScheduleSyncEvent sync = new ScheduleSyncEvent(42, anchor.plusSeconds(50), 10);
        Instant now = anchor.plusSeconds(25);
        ScheduleResolved resolved = service.resolve(anchor, standardItems, 100, sync, now);

        assertThat(resolved.mediaId()).isEqualTo(2);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isFalse();
    }

    @Test
    @DisplayName("sync with a non-positive duration is ignored")
    void testResolveSyncNonPositiveDuration() {
        ScheduleSyncEvent sync = new ScheduleSyncEvent(42, anchor.plusSeconds(20), 0);
        Instant now = anchor.plusSeconds(25);
        ScheduleResolved resolved = service.resolve(anchor, standardItems, 100, sync, now);

        assertThat(resolved.mediaId()).isEqualTo(2);
        assertThat(resolved.remaining()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.isSync()).isFalse();
    }
}
