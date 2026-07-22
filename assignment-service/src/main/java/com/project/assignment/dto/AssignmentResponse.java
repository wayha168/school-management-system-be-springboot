package com.project.assignment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.assignment.entity.Assignment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssignmentResponse {

    private UUID uuid;
    private UUID classUuid;
    private String title;
    private String description;
    private LocalDateTime dueAt;
    private UUID createdBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AssignmentResponse from(Assignment a) {
        return AssignmentResponse.builder()
                .uuid(a.getUuid())
                .classUuid(a.getClassUuid())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueAt(a.getDueAt())
                .createdBy(a.getCreatedBy())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
