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
    private String meetingUrl;
    private String provider;
    private LocalDateTime scheduledAt;
    private UUID createdBy;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
