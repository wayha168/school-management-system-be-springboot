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
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.score.ScoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/scores")
@Tag(name = "Scores & GPA")
@SecurityRequirement(name = "bearerAuth")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/me")
    @Operation(summary = "List my scores (authenticated student account)")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> myScores(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "My scores fetched",
                    scoreService.listMyScores(generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch my scores failed", ex);
        }
    }

    @GetMapping("/me/gpa")
    @Operation(summary = "Get my GPA and grade summary")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> myGpa(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "My GPA fetched",
                    scoreService.getMyGpa(generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch my GPA failed", ex);
        }
    }

    @GetMapping("/students/{studentUuid}")
    @Operation(summary = "List scores for a student (self or SCORE_READ)")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> studentScores(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "Student scores fetched",
                    scoreService.listByStudent(studentUuid, generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch student scores failed", ex);
        }
    }

    @GetMapping("/students/{studentUuid}/gpa")
    @Operation(summary = "Get GPA / letter grade for a student (self or SCORE_READ)")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> studentGpa(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "Student GPA fetched",
                    scoreService.getStudentGpa(studentUuid, generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch student GPA failed", ex);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCORE_READ')")
    @Operation(summary = "List scores (filter by class and/or generation)")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> list(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) Integer generation) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "Scores fetched",
                    scoreService.list(classUuid, generation)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch scores failed", ex);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCORE_READ')")
    @Operation(summary = "Get score by id")
    public ResponseEntity<ApiResponse<ScoreResponse>> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Score fetched", scoreService.getById(id)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch score failed", ex);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    @Operation(summary = "Create score")
    public ResponseEntity<ApiResponse<ScoreResponse>> create(@Valid @RequestBody ScoreRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Score created", scoreService.create(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Create score failed", ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    @Operation(summary = "Update score")
    public ResponseEntity<ApiResponse<ScoreResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScoreRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Score updated", scoreService.update(id, request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Update score failed", ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    @Operation(summary = "Delete score")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        try {
            scoreService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Score deleted", null));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Delete score failed", ex);
        }
    }
}
