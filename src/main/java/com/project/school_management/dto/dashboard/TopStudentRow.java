package com.project.school_management.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopStudentRow {

    private UUID studentUuid;
    private String studentName;
    private String groupKey;
    private String groupLabel;
    private BigDecimal gpa;
    private BigDecimal averagePercent;
    private String letterGrade;
}
