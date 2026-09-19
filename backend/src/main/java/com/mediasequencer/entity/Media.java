package com.mediasequencer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String kind; // "image" | "video" | "blank"

    @Column(nullable = false)
    private String url;

    @Column(name = "default_duration_seconds", nullable = false)
    private int defaultDurationSeconds;

    public Media() {
    }

    public Media(String label, String kind, String url, int defaultDurationSeconds) {
        this.label = label;
        this.kind = kind;
        this.url = url;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public Media(Long id, String label, String kind, String url, int defaultDurationSeconds) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.url = url;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public void setDefaultDurationSeconds(int defaultDurationSeconds) {
        this.defaultDurationSeconds = defaultDurationSeconds;
    }
}
