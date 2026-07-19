package com.project.school_management.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.UserRequest;
import com.project.school_management.enums.RequestCategory;
import com.project.school_management.enums.RequestStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRequestResponse {

    private UUID uuid;
    private UUID fromUserUuid;
    private String fromUserName;
    private String fromUserEmail;
    private String schoolName;
    private String schoolEmail;
    private String subject;
    private String body;
    private RequestCategory category;
    private RequestStatus status;
    private String adminReply;
    private String handledByName;
    private LocalDateTime createdAt;

    public static UserRequestResponse from(UserRequest request) {
        var user = request.getFromUser();
        var school = user != null ? user.getSchool() : null;
        return UserRequestResponse.builder()
                .uuid(request.getUuid())
                .fromUserUuid(user != null ? user.getUuid() : null)
                .fromUserName(user != null ? user.getName() : null)
                .fromUserEmail(user != null ? user.getEmail() : null)
                .schoolName(school != null ? school.getName() : null)
                .schoolEmail(school != null ? school.getEmail() : null)
                .subject(request.getSubject())
                .body(request.getBody())
                .category(request.getCategory())
                .status(request.getStatus())
                .adminReply(request.getAdminReply())
                .handledByName(request.getHandledBy() != null ? request.getHandledBy().getName() : null)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
