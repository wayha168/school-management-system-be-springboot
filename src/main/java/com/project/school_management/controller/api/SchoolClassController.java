package com.project.school_management.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.service.schoolclass.SchoolClassService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
@Tag(name = "Classes")
@SecurityRequirement(name = "basicAuth")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @PostMapping
    @Operation(summary = "Create class")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> create(@Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Class created", schoolClassService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List classes")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getAll(
            @RequestParam(required = false) UUID schoolUuid) {
        List<SchoolClassResponse> data = schoolUuid == null
                ? schoolClassService.getAll()
                : schoolClassService.getBySchool(schoolUuid);
        return ResponseEntity.ok(ApiResponse.ok("Classes fetched", data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get class by id")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Class fetched", schoolClassService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update class")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Class updated", schoolClassService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete class")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        schoolClassService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Class deleted", null));
    }
}
