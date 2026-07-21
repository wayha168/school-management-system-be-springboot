package com.project.school_management.service.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.attendance.ClassAttendanceOverview;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.enums.AttendanceStatus;
import com.project.school_management.enums.RoleName;

/**
 * Attendance service contract — method signatures only.
 * All business logic lives in {@link AttendanceServiceImpl}.
 */
public interface AttendanceService {

    List<AttendanceResponse> list(LocalDate date, UUID classUuid, UUID userUuid);

    List<AttendanceResponse> listMine(LocalDate date);

    /** List with optional role + generation filters applied in the service. */
    List<AttendanceResponse> listFiltered(
            LocalDate date,
            UUID classUuid,
            UUID userUuid,
            RoleName roleFilter,
            Integer generation);

    AttendanceResponse upsert(AttendanceRequest request);

    int markBatch(AttendanceBatchRequest request);

    /** Build batch from mark form parallel lists and save. */
    int markFromForm(
            UUID classUuid,
            LocalDate attendanceDate,
            List<UUID> studentUuids,
            List<AttendanceStatus> statuses);

    void delete(UUID id);

    ChartSeries lastMonthChart(int days);

    ChartSeries classMonthChart(UUID classUuid, int days);

    List<SchoolClassResponse> visibleClasses(DataUser account);

    /** Visible classes, optionally filtered by generation. */
    List<SchoolClassResponse> visibleClasses(DataUser account, Integer generation);

    List<SchoolClassResponse> markableClasses(DataUser account);

    /** Resolve class if allowed for the account; otherwise null. */
    SchoolClassResponse findVisibleClass(DataUser account, UUID classUuid);

    List<ClassAttendanceOverview> buildClassOverviews(List<SchoolClassResponse> classes);

    List<UserResponse> studentsInClass(UUID classUuid);

    List<UserResponse> teachersForClass(UUID classUuid);

    /** Attendance rows keyed by user uuid for one class/day. */
    Map<UUID, AttendanceResponse> attendanceByUser(LocalDate date, UUID classUuid);

    boolean canMark(DataUser account);

    boolean isManagement(DataUser account);

    /** Classes/records tab switch default based on role. */
    String resolveActiveTab(String tab, String viewAlias, DataUser account);

    RoleName parseRole(String role);
}
