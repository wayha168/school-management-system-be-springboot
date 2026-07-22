package com.project.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionRequest {

    @NotBlank
    private String content;
}
