package com.project.school_management.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.presence.PresenceSnapshot;
import com.project.school_management.service.presence.PresenceTracker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/presence")
@Tag(name = "Presence")
@SecurityRequirement(name = "bearerAuth")
public class PresenceController {

    private final PresenceTracker presenceTracker;

    public PresenceController(PresenceTracker presenceTracker) {
        this.presenceTracker = presenceTracker;
    }

    @GetMapping("/online")
    @Operation(summary = "List currently online users")
    public ResponseEntity<ApiResponse<PresenceSnapshot>> online() {
        return ResponseEntity.ok(ApiResponse.ok("Online users", presenceTracker.snapshot()));
    }
}
