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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.subject.SubjectRequest;
import com.project.school_management.dto.subject.SubjectResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.subject.SubjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subjects")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUBJECT_WRITE')")
    @Operation(summary = "Create subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Subject created", subjectService.create(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Create subject failed", ex);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBJECT_READ')")
    @Operation(summary = "List subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAll(
            @RequestParam(required = false) UUID schoolUuid) {
        try {
            List<SubjectResponse> data = schoolUuid == null
                    ? subjectService.getAll()
                    : subjectService.getBySchool(schoolUuid);
            return ResponseEntity.ok(ApiResponse.ok("Subjects fetched", data));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch subjects failed", ex);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT_READ')")
    @Operation(summary = "Get subject by id")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Subject fetched", subjectService.getById(id)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch subject failed", ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT_WRITE')")
    @Operation(summary = "Update subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Subject updated", subjectService.update(id, request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update subject failed", ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT_WRITE')")
    @Operation(summary = "Delete subject")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        try {
            subjectService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Subject deleted", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Delete subject failed", ex);
        }
    }
}
