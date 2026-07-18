package com.project.school_management.dto.permission;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionItemResponse {

    private String code;
    private String module;
    private String action;
    private String status;
    private List<String> roles;
}
