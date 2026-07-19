package com.project.school_management.dto.user;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotNull
    private UUID roleUuid;

    @NotNull
    private UUID schoolUuid;

    /** Classes the user belongs to (student) or teaches (teacher). */
    private List<UUID> classUuids;

    /** Student grade */
    private String grade;

    /** Teacher room / classroom */
    private String room;
}
