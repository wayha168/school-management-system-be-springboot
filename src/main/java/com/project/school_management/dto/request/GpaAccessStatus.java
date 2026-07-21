package com.project.school_management.dto.request;

import java.util.UUID;

import com.project.school_management.enums.RequestStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GpaAccessStatus {

    private boolean canViewGpa;
    private boolean canRequest;
    private RequestStatus status;
    private UUID requestUuid;
    private String message;
}
