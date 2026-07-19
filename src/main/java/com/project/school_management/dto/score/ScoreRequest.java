package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreRequest {

    @NotNull
    private UUID studentUuid;

    @NotNull
    private UUID classUuid;

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
