package com.project.school_management.dto.subject;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequest {

    @NotBlank
    private String name;

    private String code;

    private String description;

    @NotNull
    private UUID schoolUuid;
}
