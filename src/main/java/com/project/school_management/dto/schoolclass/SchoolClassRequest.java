package com.project.school_management.dto.schoolclass;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchoolClassRequest {

    @NotBlank
    private String name;

    private String grade;

    @NotNull
    private UUID schoolUuid;
}
