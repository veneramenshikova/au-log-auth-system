package com.lab;

import java.time.LocalDateTime;

public class Document {
    private int id;
    private String title;
    private String content;          // зашифрованный
    private String contentPlain;     // расшифрованный (для отображения)
    private String ownerId;
    private int clearanceLevel;      // 1-3 (мандатная модель)
    private String department;       // атрибутная модель
    private LocalDateTime createdAt;

    public Document(int id, String title, String content, String ownerId,
                    int clearanceLevel, String department, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ownerId = ownerId;
        this.clearanceLevel = clearanceLevel;
        this.department = department;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentPlain() { return contentPlain; }
    public void setContentPlain(String contentPlain) { this.contentPlain = contentPlain; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(int clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s (уровень: %d, отдел: %s, владелец: %s)",
                id, title, clearanceLevel, department, ownerId);
    }
}