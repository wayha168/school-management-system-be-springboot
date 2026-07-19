package com.project.school_management.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.enums.RequestCategory;
import com.project.school_management.enums.RequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_requests")
@Getter
@Setter
@NoArgsConstructor
public class UserRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_uuid", nullable = false)
    private User fromUser;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private RequestCategory category = RequestCategory.REQUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "admin_reply", length = 2000)
    private String adminReply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_uuid")
    private User handledBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
