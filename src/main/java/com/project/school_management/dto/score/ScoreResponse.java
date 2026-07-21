package com.project.school_management.dto.score;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreResponse {

    private UUID uuid;
    private UUID studentUuid;
    private String studentName;
    private UUID classUuid;
    private String className;
    private Integer generation;
    private String generationDisplay;
    private String subject;
    private String term;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String remark;
    private UUID teacherUuid;
    private String teacherName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
