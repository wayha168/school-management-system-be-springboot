package com.project.school_management.dto.attendance;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassAttendanceOverview {

    private UUID classUuid;
    private String className;
    private String grade;
    private Integer generation;
    private String generationCode;
    private String schoolName;
    private List<String> teacherNames;
    private String teachersLabel;
    private int studentCount;
}
