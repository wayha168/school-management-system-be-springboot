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
import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.school.SchoolResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.school.SchoolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schools")
@Tag(name = "Schools")
@SecurityRequirement(name = "bearerAuth")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    @Operation(summary = "Create school")
    public ResponseEntity<ApiResponse<SchoolResponse>> create(@Valid @RequestBody SchoolRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("School created", schoolService.create(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Create school failed", ex);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @Operation(summary = "List schools")
    public ResponseEntity<ApiResponse<List<SchoolResponse>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Schools fetched", schoolService.getAll()));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch schools failed", ex);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @Operation(summary = "Get school by id")
    public ResponseEntity<ApiResponse<SchoolResponse>> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("School fetched", schoolService.getById(id)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch school failed", ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    @Operation(summary = "Update school")
    public ResponseEntity<ApiResponse<SchoolResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("School updated", schoolService.update(id, request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update school failed", ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    @Operation(summary = "Delete school")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        try {
            schoolService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("School deleted", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Delete school failed", ex);
        }
    }
}
