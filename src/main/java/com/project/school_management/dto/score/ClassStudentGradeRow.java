package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassStudentGradeRow {

    private UUID studentUuid;
    private String studentName;
    private String studentEmail;
    private String grade;
    private BigDecimal gpa;
    private BigDecimal averagePercent;
    private String letterGrade;
    private int totalScores;
}
