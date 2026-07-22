package com.project.assignment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingRequest {

    @NotNull
    private UUID classUuid;

    @NotBlank
    private String title;

    /** When true, hosts can record the call and store the video in-app. */
    private boolean recordEnabled;

    private LocalDateTime scheduledAt;
}
