package com.project.school_management.dto.role;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.Role;
import com.project.school_management.enums.RoleName;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {

    private UUID uuid;
    private RoleName name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .uuid(role.getUuid())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
