package com.project.school_management.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
}
