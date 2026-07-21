package com.project.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopStudentRow {

    private UUID studentUuid;
    private String studentName;
    private String groupKey;
    private String groupLabel;
    private BigDecimal gpa;
    private BigDecimal averagePercent;
    private String letterGrade;
}
