package com.project.school_management.dto.score;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubjectGradeSummary {

    private String subject;
    private String term;
    private BigDecimal averagePercent;
    private BigDecimal gpaPoints;
    private String letterGrade;
    private int scoreCount;
}
