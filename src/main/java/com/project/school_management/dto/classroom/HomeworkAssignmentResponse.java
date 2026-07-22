package com.project.school_management.dto.classroom;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeworkAssignmentResponse {

    private UUID uuid;
    private UUID classUuid;
    private String title;
    private String description;
    private LocalDateTime dueAt;
    private UUID createdBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
