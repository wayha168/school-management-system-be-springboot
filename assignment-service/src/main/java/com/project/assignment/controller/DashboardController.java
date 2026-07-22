package com.project.assignment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.assignment.dto.ApiResponse;
import com.project.assignment.dto.ClassroomDashboard;
import com.project.assignment.service.ClassroomService;

@RestController
@RequestMapping("/api/v1/classroom")
public class DashboardController {

    private final ClassroomService classroomService;

    public DashboardController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ClassroomDashboard>> dashboard(
            @RequestParam(required = false) List<UUID> classUuid) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Classroom dashboard",
                classroomService.dashboard(classUuid == null ? List.of() : classUuid)));
    }
}
