package com.project.assessment.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectGradeSummary {

    private String subject;
    private String term;
    private BigDecimal averagePercent;
    private BigDecimal gpaPoints;
    private String letterGrade;
    private int scoreCount;
}
