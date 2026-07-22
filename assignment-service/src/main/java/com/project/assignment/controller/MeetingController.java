package com.project.assignment.controller;

import java.io.IOException;
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
import com.project.assignment.dto.MeetingRequest;
import com.project.assignment.dto.MeetingResponse;
import com.project.assignment.service.ClassroomService;
import com.project.assignment.service.ClassroomService.RecordingPayload;

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

    @GetMapping("/by-room/{roomCode}")
    public ResponseEntity<ApiResponse<MeetingResponse>> byRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(ApiResponse.ok("Meeting fetched", classroomService.getByRoomCode(roomCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingResponse>> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Meeting fetched", classroomService.getById(id)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<MeetingResponse>> end(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Meeting ended", classroomService.endMeeting(id)));
    }

    @PostMapping(path = "/{id}/recording", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MeetingResponse>> uploadRecording(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok("Recording stored", classroomService.storeRecording(id, file)));
    }

    @GetMapping("/{id}/recording")
    public ResponseEntity<Resource> downloadRecording(@PathVariable UUID id) {
        RecordingPayload payload = classroomService.loadRecording(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.downloadName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }
}
