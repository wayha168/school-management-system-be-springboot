package com.project.school_management.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Create user")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("User created", userService.create(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Create user failed", ex);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "List users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Users fetched", userService.getAll()));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch users failed", ex);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by id")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("User fetched", userService.getById(id)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch user failed", ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("User updated", userService.update(id, request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update user failed", ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Delete user")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        try {
            userService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Delete user failed", ex);
        }
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user profile image")
    public ResponseEntity<byte[]> avatar(@PathVariable UUID id) {
        SchoolImage image = userService.getProfileImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }
}
