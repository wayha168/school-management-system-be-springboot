package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreBatchRequest {

    @NotNull
    private UUID classUuid;

    /** Optional default subject when items omit subject. */
    private String subject;

    private String term;

    @DecimalMin("0.01")
    private BigDecimal maxScore;

    @NotEmpty
    @Valid
    private List<ScoreMarkItem> items;
}
