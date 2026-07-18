package com.project.school_management.dto.schoolclass;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.SchoolClass;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolClassResponse {

    private UUID uuid;
    private String name;
    private String grade;
    private UUID schoolUuid;
    private String schoolName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return SchoolClassResponse.builder()
                .uuid(schoolClass.getUuid())
                .name(schoolClass.getName())
                .grade(schoolClass.getGrade())
                .schoolUuid(schoolClass.getSchool() != null ? schoolClass.getSchool().getUuid() : null)
                .schoolName(schoolClass.getSchool() != null ? schoolClass.getSchool().getName() : null)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .build();
    }
}
