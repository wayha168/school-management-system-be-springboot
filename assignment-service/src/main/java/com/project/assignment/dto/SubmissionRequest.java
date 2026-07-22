package com.project.assignment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionRequest {

    /** Optional text / link. Required only when no file is uploaded. */
    private String content;
}
