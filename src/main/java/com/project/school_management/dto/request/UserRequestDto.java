package com.project.school_management.dto.request;

import com.project.school_management.enums.RequestCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    @NotNull
    private RequestCategory category;
}
