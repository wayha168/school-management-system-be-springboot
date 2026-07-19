package com.project.school_management.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
import com.project.school_management.dto.permission.PermissionAssignRequest;
import com.project.school_management.dto.permission.PermissionItemResponse;
import com.project.school_management.dto.permission.RolePermissionRequest;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.permission.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(summary = "List all permissions")
    public ResponseEntity<ApiResponse<List<PermissionItemResponse>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Permissions fetched", permissionService.getAll()));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch permissions failed", ex);
        }
    }

    @GetMapping("/roles/{roleUuid}")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(summary = "List permissions for a role")
    public ResponseEntity<ApiResponse<List<String>>> getByRole(@PathVariable UUID roleUuid) {
        try {
            return ResponseEntity
                    .ok(ApiResponse.ok("Role permissions fetched", permissionService.getPermissionsForRole(roleUuid)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch role permissions failed", ex);
        }
    }

    @PutMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Replace permissions for a role")
    public ResponseEntity<ApiResponse<Void>> replace(@Valid @RequestBody RolePermissionRequest request) {
        try {
            permissionService.replaceRolePermissions(request);
            return ResponseEntity.ok(ApiResponse.ok("Role permissions updated", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update role permissions failed", ex);
        }
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Assign one permission to a role")
    public ResponseEntity<ApiResponse<Void>> assign(@Valid @RequestBody PermissionAssignRequest request) {
        try {
            permissionService.assign(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Permission assigned", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Assign permission failed", ex);
        }
    }

    @DeleteMapping("/roles/{roleUuid}/{permission}")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Revoke one permission from a role")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable UUID roleUuid,
            @PathVariable String permission) {
        try {
            permissionService.revoke(roleUuid, permission);
            return ResponseEntity.ok(ApiResponse.ok("Permission revoked", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Revoke permission failed", ex);
        }
    }
}
