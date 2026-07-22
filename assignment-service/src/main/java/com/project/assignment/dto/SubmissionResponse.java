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
    private boolean hasAttachment;
    private String attachmentOriginalName;
    private String attachmentContentType;
    private Long attachmentBytes;
    private boolean imageAttachment;
    private LocalDateTime submittedAt;

    public static SubmissionResponse from(AssignmentSubmission s) {
        String ct = s.getAttachmentContentType();
        boolean image = ct != null && ct.toLowerCase().startsWith("image/");
        return SubmissionResponse.builder()
                .uuid(s.getUuid())
                .assignmentUuid(s.getAssignmentUuid())
                .studentUuid(s.getStudentUuid())
                .content(s.getContent())
                .hasAttachment(s.isHasAttachment())
                .attachmentOriginalName(s.getAttachmentOriginalName())
                .attachmentContentType(ct)
                .attachmentBytes(s.getAttachmentBytes())
                .imageAttachment(image)
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
