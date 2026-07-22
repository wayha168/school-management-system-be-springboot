package com.project.school_management.dto.subject;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.Subject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectResponse {

    private UUID uuid;
    private String name;
    private String code;
    private String description;
    private UUID schoolUuid;
    private String schoolName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubjectResponse from(Subject subject) {
        return SubjectResponse.builder()
                .uuid(subject.getUuid())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .schoolUuid(subject.getSchool() != null ? subject.getSchool().getUuid() : null)
                .schoolName(subject.getSchool() != null ? subject.getSchool().getName() : null)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
