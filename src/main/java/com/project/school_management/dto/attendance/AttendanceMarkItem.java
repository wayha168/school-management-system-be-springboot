package com.project.school_management.dto.attendance;

import java.util.UUID;

import com.project.school_management.enums.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceMarkItem {

    @NotNull
    private UUID userUuid;

    @NotNull
    private AttendanceStatus status;

    private String remark;
}
