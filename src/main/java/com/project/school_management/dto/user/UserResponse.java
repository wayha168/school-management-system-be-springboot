package com.project.school_management.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private UUID uuid;
    private String name;
    private String email;
    private RoleName role;
    private UUID roleUuid;
    private UUID schoolUuid;
    private String schoolName;
    private UUID classUuid;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .uuid(user.getUuid())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .roleUuid(user.getRole() != null ? user.getRole().getUuid() : null)
                .schoolUuid(user.getSchool() != null ? user.getSchool().getUuid() : null)
                .schoolName(user.getSchool() != null ? user.getSchool().getName() : null)
                .classUuid(user.getSchoolClass() != null ? user.getSchoolClass().getUuid() : null)
                .className(user.getSchoolClass() != null ? user.getSchoolClass().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
