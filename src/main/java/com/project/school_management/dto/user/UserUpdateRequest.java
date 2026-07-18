package com.project.school_management.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    private String password;

    @NotNull
    private UUID roleUuid;

    @NotNull
    private UUID schoolUuid;

    private UUID classUuid;
}
