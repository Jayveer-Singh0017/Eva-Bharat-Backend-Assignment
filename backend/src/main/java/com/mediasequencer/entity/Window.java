package com.mediasequencer.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "windows")
public class Window {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "\"position\"", nullable = false)
    private int position;

    @OneToMany(mappedBy = "window", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WindowItem> items = new ArrayList<>();

    public Window() {
    }

    public Window(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public Window(Long id, String name, int position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<WindowItem> getItems() {
        return items;
    }

    public void setItems(List<WindowItem> items) {
        this.items = items;
    }
}
