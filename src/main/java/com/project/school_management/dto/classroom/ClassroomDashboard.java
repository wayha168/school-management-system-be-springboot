package com.project.school_management.dto.classroom;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomDashboard {

    private List<MeetingResponse> activeMeetings = new ArrayList<>();
    private List<HomeworkAssignmentResponse> openAssignments = new ArrayList<>();
}
