package com.project.assessment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.project.assessment.entity.StudentScore;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreResponse {

    private UUID uuid;
    private UUID studentUuid;
    private String studentName;
    private String studentEmail;
    private String studentGrade;
    private UUID schoolUuid;
    private UUID classUuid;
    private String className;
    private Integer generation;
    private String generationDisplay;
    private Integer academicYear;
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
        Integer generation = score.getGeneration();
        Integer year = score.getAcademicYear();
        return ScoreResponse.builder()
                .uuid(score.getUuid())
                .studentUuid(score.getStudentUuid())
                .studentName(score.getStudentName())
                .studentEmail(score.getStudentEmail())
                .studentGrade(score.getStudentGrade())
                .schoolUuid(score.getSchoolUuid())
                .classUuid(score.getSchoolClassUuid())
                .className(score.getClassName())
                .generation(generation)
                .generationDisplay(display(generation, year))
                .academicYear(year)
                .subject(score.getSubject())
                .term(score.getTerm())
                .score(score.getScore())
                .maxScore(score.getMaxScore())
                .remark(score.getRemark())
                .teacherUuid(score.getTeacherUuid())
                .teacherName(score.getTeacherName())
                .createdAt(score.getCreatedAt())
                .updatedAt(score.getUpdatedAt())
                .build();
    }

    private static String display(Integer generation, Integer academicYear) {
        if (generation == null && academicYear == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (generation != null) {
            sb.append('G').append(generation);
        }
        if (academicYear != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(academicYear);
        }
        return sb.toString();
    }
}
