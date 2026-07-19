package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentGpaResponse {

    private UUID studentUuid;
    private String studentName;
    private String studentEmail;
    private Integer generation;
    private String term;
    private BigDecimal gpa;
    private BigDecimal averagePercent;
    private String letterGrade;
    private int totalScores;
    private List<SubjectGradeSummary> subjects;
    private List<ScoreResponse> scores;
}
