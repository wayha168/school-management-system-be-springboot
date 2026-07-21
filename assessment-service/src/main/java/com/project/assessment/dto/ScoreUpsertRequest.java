package com.project.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreUpsertRequest {

    @NotNull
    private UUID studentUuid;
    private String studentName;
    private String studentEmail;
    private String studentGrade;

    private UUID schoolUuid;

    @NotNull
    private UUID classUuid;
    private String className;
    private Integer generation;
    private Integer academicYear;

    @NotNull
    private UUID teacherUuid;
    private String teacherName;

    @NotBlank
    private String subject;
    private String term;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal score;

    @DecimalMin("0.01")
    private BigDecimal maxScore;

    private String remark;
}
