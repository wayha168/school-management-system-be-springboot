package com.project.school_management.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.auth.LoginRequest;
import com.project.school_management.dto.auth.LoginResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.auth.AuthService;
import com.project.school_management.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Login success", response));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Login failed", ex);
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current account details")
    public ResponseEntity<ApiResponse<DataUser>> me(Authentication authentication) {
        try {
            DataUser account = userService.getAccountByEmail(authentication.getName());
            return ResponseEntity.ok(ApiResponse.ok("Account fetched", account));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Unable to load account details", ex);
        }
    }
}
