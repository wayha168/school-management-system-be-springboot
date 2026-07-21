package com.project.assessment.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GpaSummaryStats {

    private BigDecimal averageGpa;
    private BigDecimal averagePercent;
    private int studentsWithScores;
    private int totalScoreRows;
    private String topLetter;
}
