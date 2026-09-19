package com.mediasequencer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "window_items", indexes = {
        @Index(name = "idx_window_items_window_position", columnList = "window_id, position")
})
public class WindowItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "window_id", nullable = false)
    private Window window;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "\"position\"", nullable = false)
    private int position;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    public WindowItem() {
    }

    public WindowItem(Window window, Media media, int position, int durationSeconds) {
        this.window = window;
        this.media = media;
        this.position = position;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
