package com.project.assessment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "student_scores",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_uuid", "school_class_uuid", "subject", "term"}))
@Getter
@Setter
@NoArgsConstructor
public class StudentScore {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "student_uuid", nullable = false)
    private UUID studentUuid;

    @Column(name = "student_name", length = 150)
    private String studentName;

    @Column(name = "student_email", length = 150)
    private String studentEmail;

    @Column(name = "student_grade", length = 50)
    private String studentGrade;

    @Column(name = "school_uuid")
    private UUID schoolUuid;

    @Column(name = "school_class_uuid", nullable = false)
    private UUID schoolClassUuid;

    @Column(name = "class_name", length = 150)
    private String className;

    @Column(name = "generation")
    private Integer generation;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "teacher_uuid", nullable = false)
    private UUID teacherUuid;

    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "term", nullable = false, length = 50)
    private String term;

    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore = BigDecimal.valueOf(100);

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
