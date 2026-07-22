package com.project.assignment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.assignment.dto.ApiResponse;
import com.project.assignment.dto.AssignmentRequest;
import com.project.assignment.dto.AssignmentResponse;
import com.project.assignment.dto.SubmissionRequest;
import com.project.assignment.dto.SubmissionResponse;
import com.project.assignment.service.ClassroomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final ClassroomService classroomService;

    public AssignmentController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(@Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Assignment created", classroomService.createAssignment(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(@RequestParam UUID classUuid) {
        return ResponseEntity.ok(ApiResponse.ok("Assignments fetched", classroomService.listAssignments(classUuid)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<AssignmentResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Assignment closed", classroomService.closeAssignment(id)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @PathVariable UUID id,
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Submitted", classroomService.submit(id, request)));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> submissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Submissions fetched", classroomService.listSubmissions(id)));
    }
}
