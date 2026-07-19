package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.StudentScore;
import com.project.school_management.util.GenerationLabels;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreResponse {

    private UUID uuid;
    private UUID studentUuid;
    private String studentName;
    private UUID classUuid;
    private String className;
    private Integer generation;
    private String generationDisplay;
    private String subject;
    private String term;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String remark;
    private UUID teacherUuid;
    private String teacherName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ScoreResponse from(StudentScore score) {
        var schoolClass = score.getSchoolClass();
        Integer generation = schoolClass != null ? schoolClass.getGeneration() : null;
        Integer year = schoolClass != null ? schoolClass.getAcademicYear() : null;
        return ScoreResponse.builder()
                .uuid(score.getUuid())
                .studentUuid(score.getStudent() != null ? score.getStudent().getUuid() : null)
                .studentName(score.getStudent() != null ? score.getStudent().getName() : null)
                .classUuid(schoolClass != null ? schoolClass.getUuid() : null)
                .className(schoolClass != null ? schoolClass.getName() : null)
                .generation(generation)
                .generationDisplay(GenerationLabels.display(generation, year))
                .subject(score.getSubject())
                .term(score.getTerm())
                .score(score.getScore())
                .maxScore(score.getMaxScore())
                .remark(score.getRemark())
                .teacherUuid(score.getTeacher() != null ? score.getTeacher().getUuid() : null)
                .teacherName(score.getTeacher() != null ? score.getTeacher().getName() : null)
                .createdAt(score.getCreatedAt())
                .updatedAt(score.getUpdatedAt())
                .build();
    }
}
