package com.project.school_management.controller.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.service.attendance.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @Operation(summary = "My attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok("My attendance fetched", attendanceService.listMine(date)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @Operation(summary = "List attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) UUID userUuid) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Attendance fetched",
                attendanceService.list(date, classUuid, userUuid)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    @Operation(summary = "Upsert attendance row")
    public ResponseEntity<ApiResponse<AttendanceResponse>> upsert(@Valid @RequestBody AttendanceRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Attendance saved", attendanceService.upsert(request)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ErrorRuntime("Save attendance failed", ex);
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    @Operation(summary = "Mark class attendance for a day")
    public ResponseEntity<ApiResponse<Integer>> batch(@Valid @RequestBody AttendanceBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Attendance marked", attendanceService.markBatch(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    @Operation(summary = "Delete attendance")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Attendance deleted", null));
    }
}
