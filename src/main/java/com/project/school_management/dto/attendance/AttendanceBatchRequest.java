package com.project.school_management.dto.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceBatchRequest {

    @NotNull
    private UUID classUuid;

    @NotNull
    private LocalDate attendanceDate;

    @NotEmpty
    @Valid
    private List<AttendanceMarkItem> items;
}
