package com.project.school_management.dto.attendance;

import java.time.LocalDate;
import java.util.UUID;

import com.project.school_management.enums.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceRequest {

    @NotNull
    private UUID userUuid;

    private UUID classUuid;

    @NotNull
    private LocalDate attendanceDate;

    @NotNull
    private AttendanceStatus status;

    private String remark;
}
