package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreMarkItem {

    @NotNull
    private UUID studentUuid;

    /** Subject for this cell; falls back to batch-level subject when blank. */
    private String subject;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal score;

    private String remark;
}
