package com.project.school_management.service.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.dashboard.ChartSeries;

public interface AttendanceService {

    List<AttendanceResponse> list(LocalDate date, UUID classUuid, UUID userUuid);

    List<AttendanceResponse> listMine(LocalDate date);

    AttendanceResponse upsert(AttendanceRequest request);

    int markBatch(AttendanceBatchRequest request);

    void delete(UUID id);

    /** Daily P/A/L counts for the last {@code days} days (inclusive of today). */
    ChartSeries lastMonthChart(int days);

    /** Daily P/A/L counts for one class over the last {@code days} days. */
    ChartSeries classMonthChart(UUID classUuid, int days);
}
