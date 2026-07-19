package com.project.school_management.dto.request;

import com.project.school_management.enums.RequestStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestReplyDto {

    @NotNull
    private RequestStatus status;

    private String adminReply;
}
