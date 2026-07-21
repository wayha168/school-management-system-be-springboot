package com.project.school_management.service.auth;

import com.project.school_management.dto.auth.LoginRequest;
import com.project.school_management.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
