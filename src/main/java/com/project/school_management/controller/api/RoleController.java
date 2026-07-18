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
import com.project.school_management.dto.role.RoleRequest;
import com.project.school_management.dto.role.RoleResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.role.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Create role")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Role created", roleService.create(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Create role failed", ex);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(summary = "List roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Roles fetched", roleService.getAll()));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch roles failed", ex);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(summary = "Get role by id")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Role fetched", roleService.getById(id)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch role failed", ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Update role")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Role updated", roleService.update(id, request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update role failed", ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    @Operation(summary = "Delete role")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        try {
            roleService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Role deleted", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Delete role failed", ex);
        }
    }
}
