package com.project.assignment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.assignment.entity.ClassMeeting;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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

    public static MeetingResponse from(ClassMeeting m) {
        String joinPath = m.getMeetingUrl();
        if (joinPath == null || joinPath.isBlank()) {
            joinPath = m.getRoomCode() != null ? joinPathFor(m.getRoomCode()) : null;
        }
        return MeetingResponse.builder()
                .uuid(m.getUuid())
                .classUuid(m.getClassUuid())
                .title(m.getTitle())
                .roomCode(m.getRoomCode())
                .meetingUrl(joinPath)
                .joinPath(joinPath)
                .provider(m.getProvider())
                .recordEnabled(m.isRecordEnabled())
                .hasRecording(m.isHasRecording())
                .recordingContentType(m.getRecordingContentType())
                .recordingBytes(m.getRecordingBytes())
                .recordedAt(m.getRecordedAt())
                .scheduledAt(m.getScheduledAt())
                .createdBy(m.getCreatedBy())
                .active(m.isActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public static String joinPathFor(String roomCode) {
        return "/admin/classroom/call/" + roomCode;
    }
}
