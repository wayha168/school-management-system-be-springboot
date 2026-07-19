package com.project.school_management.dto.user;

import java.util.UUID;

import com.project.school_management.entities.SchoolClass;
import com.project.school_management.util.GenerationLabels;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserClassItem {

    private UUID uuid;
    private String name;
    private Integer generation;
    private Integer academicYear;
    private String generationCode;
    private String generationDisplay;
    private String grade;

    public static UserClassItem from(SchoolClass schoolClass) {
        Integer generation = schoolClass.getGeneration();
        Integer academicYear = schoolClass.getAcademicYear();
        return UserClassItem.builder()
                .uuid(schoolClass.getUuid())
                .name(schoolClass.getName())
                .generation(generation)
                .academicYear(academicYear)
                .generationCode(GenerationLabels.code(generation))
                .generationDisplay(GenerationLabels.display(generation, academicYear))
                .grade(schoolClass.getGrade())
                .build();
    }
}
