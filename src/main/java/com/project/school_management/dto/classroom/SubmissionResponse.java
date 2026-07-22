package com.project.school_management.dto.classroom;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionResponse {

    private UUID uuid;
    private UUID assignmentUuid;
    private UUID studentUuid;
    private String content;
    private LocalDateTime submittedAt;
}
