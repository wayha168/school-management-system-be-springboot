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
    private String meetingUrl;
    private String provider;
    private LocalDateTime scheduledAt;
    private UUID createdBy;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MeetingResponse from(ClassMeeting m) {
        return MeetingResponse.builder()
                .uuid(m.getUuid())
                .classUuid(m.getClassUuid())
                .title(m.getTitle())
                .meetingUrl(m.getMeetingUrl())
                .provider(m.getProvider())
                .scheduledAt(m.getScheduledAt())
                .createdBy(m.getCreatedBy())
                .active(m.isActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
