package com.project.school_management.dto.permission;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionAssignRequest {

    @NotNull
    private UUID roleUuid;

    @NotBlank
    private String permission;
}
