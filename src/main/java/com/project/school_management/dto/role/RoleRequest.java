package com.project.school_management.dto.role;

import com.project.school_management.enums.RoleName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {

    @NotNull
    private RoleName name;

    @NotBlank
    private String description;
}
