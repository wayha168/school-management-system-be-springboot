package com.project.assessment.dto;

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
public class ScoreBatchUpsertRequest {

    @NotNull
    private UUID classUuid;
    private String className;
    private Integer generation;
    private Integer academicYear;
    private UUID schoolUuid;

    @NotNull
    private UUID teacherUuid;
    private String teacherName;

    private String subject;
    private String term;

    @DecimalMin("0.01")
    private BigDecimal maxScore;

    @NotEmpty
    @Valid
    private List<ScoreBatchItem> items;

    @Getter
    @Setter
    public static class ScoreBatchItem {
        @NotNull
        private UUID studentUuid;
        private String studentName;
        private String studentEmail;
        private String studentGrade;
        private String subject;
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal score;
        private String remark;
    }
}
