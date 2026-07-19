package com.project.school_management.controller.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.score.ScoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/grades")
@Tag(name = "Grades (GPA)")
@SecurityRequirement(name = "bearerAuth")
public class GradeController {

    private final ScoreService scoreService;

    public GradeController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/me")
    @Operation(summary = "My GPA / grade summary")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> myGrades(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("My grades fetched", scoreService.getMyGpa(generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch my grades failed", ex);
        }
    }

    @GetMapping("/students/{studentUuid}")
    @Operation(summary = "Student GPA / grade summary (self or SCORE_READ)")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> studentGrades(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "Student grades fetched",
                    scoreService.getStudentGpa(studentUuid, generation, term)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Fetch student grades failed", ex);
        }
    }
}
