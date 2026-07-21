package com.project.assessment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gpa_access_grants")
@Getter
@Setter
@NoArgsConstructor
public class GpaAccessGrant {

    @Id
    @Column(name = "student_uuid", nullable = false)
    private UUID studentUuid;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_by_uuid")
    private UUID approvedByUuid;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
