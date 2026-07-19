package com.project.school_management.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.AttendanceRecord;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    @Query("""
            SELECT DISTINCT a FROM AttendanceRecord a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.role
            LEFT JOIN FETCH a.schoolClass c
            LEFT JOIN FETCH c.school
            LEFT JOIN FETCH a.markedBy
            WHERE a.uuid = :id
            """)
    Optional<AttendanceRecord> findDetailedById(@Param("id") UUID id);

    /**
     * Use boolean flags instead of {@code :param IS NULL} — PostgreSQL cannot
     * infer JDBC types for null parameters in that pattern.
     */
    @Query("""
            SELECT DISTINCT a FROM AttendanceRecord a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.role
            LEFT JOIN FETCH a.schoolClass c
            LEFT JOIN FETCH c.school
            LEFT JOIN FETCH a.markedBy
            WHERE (:filterDate = false OR a.attendanceDate = :date)
              AND (:filterClass = false OR c.uuid = :classUuid)
              AND (:filterUser = false OR u.uuid = :userUuid)
            ORDER BY a.attendanceDate DESC, u.name
            """)
    List<AttendanceRecord> findFiltered(
            @Param("filterDate") boolean filterDate,
            @Param("date") LocalDate date,
            @Param("filterClass") boolean filterClass,
            @Param("classUuid") UUID classUuid,
            @Param("filterUser") boolean filterUser,
            @Param("userUuid") UUID userUuid);

    Optional<AttendanceRecord> findByUser_UuidAndSchoolClass_UuidAndAttendanceDate(
            UUID userUuid, UUID classUuid, LocalDate date);

    Optional<AttendanceRecord> findByUser_UuidAndSchoolClassIsNullAndAttendanceDate(
            UUID userUuid, LocalDate date);
}
