package com.project.school_management.dto.auth;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private String email;
    private String role;
    private List<String> permissions;
}
