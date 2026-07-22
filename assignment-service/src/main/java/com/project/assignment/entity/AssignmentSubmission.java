package com.project.assignment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assignment_submissions", uniqueConstraints = @UniqueConstraint(
        name = "uq_assignment_student",
        columnNames = {"assignment_uuid", "student_uuid"}))
@Getter
@Setter
public class AssignmentSubmission {

    @Id
    private UUID uuid;

    @Column(name = "assignment_uuid", nullable = false)
    private UUID assignmentUuid;

    @Column(name = "student_uuid", nullable = false)
    private UUID studentUuid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
