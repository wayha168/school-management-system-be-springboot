package com.project.assessment.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.assessment.dto.ApiResponse;
import com.project.assessment.dto.GpaAccessRequest;
import com.project.assessment.dto.GpaAccessResponse;
import com.project.assessment.service.GpaAccessService;

@RestController
@RequestMapping("/internal/v1/gpa-access")
public class InternalGpaAccessController {

    private final GpaAccessService gpaAccessService;

    public InternalGpaAccessController(GpaAccessService gpaAccessService) {
        this.gpaAccessService = gpaAccessService;
    }

    @PutMapping("/{studentUuid}")
    public ResponseEntity<ApiResponse<GpaAccessResponse>> setAccess(
            @PathVariable UUID studentUuid,
            @RequestBody GpaAccessRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "GPA access updated",
                gpaAccessService.setAccess(studentUuid, request)));
    }

    @GetMapping("/{studentUuid}")
    public ResponseEntity<ApiResponse<GpaAccessResponse>> getAccess(@PathVariable UUID studentUuid) {
        return ResponseEntity.ok(ApiResponse.ok(
                "GPA access fetched",
                gpaAccessService.getAccess(studentUuid)));
    }
}
