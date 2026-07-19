package com.project.school_management.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_uuid", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_uuid", nullable = false)
    private SchoolMag school;

    /** Students enroll in many classes; teachers teach many classes. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_school_classes",
            joinColumns = @JoinColumn(name = "user_uuid"),
            inverseJoinColumns = @JoinColumn(name = "school_class_uuid"))
    private List<SchoolClass> schoolClasses = new ArrayList<>();

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "room", length = 100)
    private String room;

    /** Monthly salary for teacher / staff (and other paid roles). */
    @Column(name = "salary", precision = 14, scale = 2)
    private BigDecimal salary;

    @Lob
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "profile_image_data")
    private byte[] profileImageData;

    @Column(name = "profile_image_content_type", length = 100)
    private String profileImageContentType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User(String name, String email, String password, Role role, SchoolMag school) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.school = school;
    }

    public boolean hasProfileImage() {
        return profileImageData != null && profileImageData.length > 0;
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
