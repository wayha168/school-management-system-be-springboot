package com.project.school_management.dto.schoolclass;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.SchoolClass;
import com.project.school_management.util.GenerationLabels;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolClassResponse {

    private UUID uuid;
    private String name;
    private String grade;
    private Integer generation;
    private Integer academicYear;
    private String generationCode;
    private String generationLabel;
    private String generationDisplay;
    private UUID schoolUuid;
    private String schoolName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        Integer generation = schoolClass.getGeneration();
        Integer academicYear = schoolClass.getAcademicYear();
        return SchoolClassResponse.builder()
                .uuid(schoolClass.getUuid())
                .name(schoolClass.getName())
                .grade(schoolClass.getGrade())
                .generation(generation)
                .academicYear(academicYear)
                .generationCode(GenerationLabels.code(generation))
                .generationLabel(GenerationLabels.label(generation))
                .generationDisplay(GenerationLabels.display(generation, academicYear))
                .schoolUuid(schoolClass.getSchool() != null ? schoolClass.getSchool().getUuid() : null)
                .schoolName(schoolClass.getSchool() != null ? schoolClass.getSchool().getName() : null)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .build();
    }
}
