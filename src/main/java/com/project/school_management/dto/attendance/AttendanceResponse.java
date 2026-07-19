package com.project.school_management.dto.attendance;

import java.time.LocalDate;
import java.util.UUID;

import com.project.school_management.entities.AttendanceRecord;
import com.project.school_management.enums.AttendanceStatus;
import com.project.school_management.enums.RoleName;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceResponse {

    private UUID uuid;
    private UUID userUuid;
    private String userName;
    private String userEmail;
    private RoleName userRole;
    private UUID classUuid;
    private String className;
    private Integer generation;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private String remark;
    private String markedByName;

    public static AttendanceResponse from(AttendanceRecord record) {
        var schoolClass = record.getSchoolClass();
        var user = record.getUser();
        RoleName role = user != null && user.getRole() != null ? user.getRole().getName() : null;
        return AttendanceResponse.builder()
                .uuid(record.getUuid())
                .userUuid(user != null ? user.getUuid() : null)
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userRole(role)
                .classUuid(schoolClass != null ? schoolClass.getUuid() : null)
                .className(schoolClass != null ? schoolClass.getName() : null)
                .generation(schoolClass != null ? schoolClass.getGeneration() : null)
                .attendanceDate(record.getAttendanceDate())
                .status(record.getStatus())
                .remark(record.getRemark())
                .markedByName(record.getMarkedBy() != null ? record.getMarkedBy().getName() : null)
                .build();
    }
}
