package com.project.assignment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.assignment.dto.ApiResponse;
import com.project.assignment.dto.AssignmentRequest;
import com.project.assignment.dto.AssignmentResponse;
import com.project.assignment.dto.SubmissionRequest;
import com.project.assignment.dto.SubmissionResponse;
import com.project.assignment.service.ClassroomService;
import com.project.assignment.service.ClassroomService.RecordingPayload;

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

    @PostMapping(path = "/{id}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitJson(
            @PathVariable UUID id,
            @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Submitted", classroomService.submit(id, request)));
    }

    @PostMapping(path = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitMultipart(
            @PathVariable UUID id,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Submitted", classroomService.submit(id, content, file)));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> submissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Submissions fetched", classroomService.listSubmissions(id)));
    }

    @GetMapping("/{id}/submissions/{submissionId}/attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID submissionId) {
        RecordingPayload payload = classroomService.loadSubmissionAttachment(id, submissionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.downloadName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }
}
