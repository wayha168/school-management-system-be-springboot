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
import com.project.assignment.dto.MeetingRequest;
import com.project.assignment.dto.MeetingResponse;
import com.project.assignment.service.ClassroomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/meetings")
public class MeetingController {

    private final ClassroomService classroomService;

    public MeetingController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> create(@Valid @RequestBody MeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Meeting created", classroomService.createMeeting(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> list(@RequestParam UUID classUuid) {
        return ResponseEntity.ok(ApiResponse.ok("Meetings fetched", classroomService.listMeetings(classUuid)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<MeetingResponse>> end(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Meeting ended", classroomService.endMeeting(id)));
    }
}
