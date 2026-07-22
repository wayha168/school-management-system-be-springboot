package com.project.assignment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.assignment.entity.AssignmentSubmission;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmissionResponse {

    private UUID uuid;
    private UUID assignmentUuid;
    private UUID studentUuid;
    private String content;
    private LocalDateTime submittedAt;

    public static SubmissionResponse from(AssignmentSubmission s) {
        return SubmissionResponse.builder()
                .uuid(s.getUuid())
                .assignmentUuid(s.getAssignmentUuid())
                .studentUuid(s.getStudentUuid())
                .content(s.getContent())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
