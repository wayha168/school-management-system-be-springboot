package com.project.school_management.dto.classroom;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingResponse {

    private UUID uuid;
    private UUID classUuid;
    private String title;
    private String roomCode;
    private String meetingUrl;
    private String joinPath;
    private String provider;
    private boolean recordEnabled;
    private boolean hasRecording;
    private String recordingContentType;
    private Long recordingBytes;
    private LocalDateTime recordedAt;
    private LocalDateTime scheduledAt;
    private UUID createdBy;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String resolveJoinPath() {
        if (joinPath != null && !joinPath.isBlank()) {
            return joinPath;
        }
        if (meetingUrl != null && meetingUrl.startsWith("/")) {
            return meetingUrl;
        }
        if (roomCode != null && !roomCode.isBlank()) {
            return "/admin/classroom/call/" + roomCode;
        }
        return meetingUrl;
    }
}
