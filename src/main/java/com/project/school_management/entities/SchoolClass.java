package com.project.school_management.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_classes")
@Getter
@Setter
@NoArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "grade")
    private String grade;

    /** Generation id / cohort number (e.g. 9 → G9, 9th generation). */
    @Column(name = "generation", nullable = false)
    private Integer generation;

    /** Academic year for this generation (e.g. 2025). */
    @Column(name = "academic_year")
    private Integer academicYear;

    /** Short code students/users enter to join this class group. */
    @Column(name = "join_code", unique = true, length = 12)
    private String joinCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_uuid", nullable = false)
    private SchoolMag school;

    @ManyToMany(mappedBy = "schoolClasses")
    private List<User> users = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "school_class_subjects", joinColumns = @JoinColumn(name = "school_class_uuid"))
    @Column(name = "subject", nullable = false, length = 100)
    @OrderColumn(name = "sort_order")
    private List<String> subjects = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SchoolClass(String name, String grade, Integer generation, Integer academicYear) {
        this.name = name;
        this.grade = grade;
        this.generation = generation;
        this.academicYear = academicYear;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
