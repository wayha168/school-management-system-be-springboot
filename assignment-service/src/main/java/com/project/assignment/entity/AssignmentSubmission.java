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

    /** Optional text / link answer. */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "has_attachment", nullable = false)
    private boolean hasAttachment = false;

    @Column(name = "attachment_stored_name", length = 255)
    private String attachmentStoredName;

    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    @Column(name = "attachment_content_type", length = 120)
    private String attachmentContentType;

    @Column(name = "attachment_bytes")
    private Long attachmentBytes;

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
