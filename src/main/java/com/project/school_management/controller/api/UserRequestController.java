package com.project.school_management.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.request.UserRequestDto;
import com.project.school_management.dto.request.UserRequestReplyDto;
import com.project.school_management.dto.request.UserRequestResponse;
import com.project.school_management.enums.RequestStatus;
import com.project.school_management.service.request.UserRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Requests & Complaints")
@SecurityRequirement(name = "bearerAuth")
public class UserRequestController {

    private final UserRequestService userRequestService;

    public UserRequestController(UserRequestService userRequestService) {
        this.userRequestService = userRequestService;
    }

    @PostMapping
    @Operation(summary = "Submit a request or complaint")
    public ResponseEntity<ApiResponse<UserRequestResponse>> create(@Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Request submitted", userRequestService.create(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "My requests")
    public ResponseEntity<ApiResponse<List<UserRequestResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok("My requests fetched", userRequestService.listMine()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REQUEST_WRITE')")
    @Operation(summary = "Staff inbox")
    public ResponseEntity<ApiResponse<List<UserRequestResponse>>> list(
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Requests fetched", userRequestService.listAll(status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request by id")
    public ResponseEntity<ApiResponse<UserRequestResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Request fetched", userRequestService.getById(id)));
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('REQUEST_WRITE')")
    @Operation(summary = "Reply / update status")
    public ResponseEntity<ApiResponse<UserRequestResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequestReplyDto reply) {
        return ResponseEntity.ok(ApiResponse.ok("Request updated", userRequestService.reply(id, reply)));
    }
}
