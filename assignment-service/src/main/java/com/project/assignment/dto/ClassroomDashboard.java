package com.project.assignment.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassroomDashboard {

    private List<MeetingResponse> activeMeetings;
    private List<AssignmentResponse> openAssignments;
}
