package com.project.school_management.dto.permission;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolePermissionRequest {

    @NotNull
    private UUID roleUuid;

    @NotEmpty
    private List<String> permissions;
}
