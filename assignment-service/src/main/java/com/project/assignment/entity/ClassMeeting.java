package com.project.assignment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "class_meetings")
@Getter
@Setter
public class ClassMeeting {

    @Id
    private UUID uuid;

    @Column(name = "class_uuid", nullable = false)
    private UUID classUuid;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "meeting_url", nullable = false, length = 1000)
    private String meetingUrl;

    @Column(nullable = false, length = 20)
    private String provider = "OTHER";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
