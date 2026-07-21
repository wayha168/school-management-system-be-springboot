package com.project.school_management.dto.schoolclass;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> subjects;
    /** Teachers assigned to this class. */
    private List<UUID> teacherUuids;
    private List<String> teacherNames;
    private String teachersLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return from(schoolClass, List.of(), List.of());
    }

    public static SchoolClassResponse from(
            SchoolClass schoolClass,
            List<UUID> teacherUuids,
            List<String> teacherNames) {
        Integer generation = schoolClass.getGeneration();
        Integer academicYear = schoolClass.getAcademicYear();
        List<String> subjects = schoolClass.getSubjects() == null
                ? List.of()
                : schoolClass.getSubjects().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim)
                        .toList();
        List<UUID> teachers = teacherUuids == null ? List.of() : List.copyOf(teacherUuids);
        List<String> names = teacherNames == null ? List.of() : List.copyOf(teacherNames);
        String label = names.isEmpty() ? "Unassigned" : String.join(", ", names);
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
                .subjects(new ArrayList<>(subjects))
                .teacherUuids(new ArrayList<>(teachers))
                .teacherNames(new ArrayList<>(names))
                .teachersLabel(label)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .build();
    }
}
