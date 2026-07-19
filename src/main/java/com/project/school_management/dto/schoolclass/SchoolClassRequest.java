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

    /** Generation id (e.g. 9 for G9). */
    @NotNull
    private Integer generation;

    /** Academic year (e.g. 2025). */
    private Integer academicYear;

    @NotNull
    private UUID schoolUuid;
}
