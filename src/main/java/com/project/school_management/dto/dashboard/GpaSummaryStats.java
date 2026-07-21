package com.project.school_management.dto.dashboard;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class GpaSummaryStats {

    private BigDecimal averageGpa;
    private BigDecimal averagePercent;
    private int studentsWithScores;
    private int totalScoreRows;
    private String topLetter;
}
