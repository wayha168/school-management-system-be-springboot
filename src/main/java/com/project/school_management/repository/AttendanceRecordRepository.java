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

    @Query("""
            SELECT DISTINCT a FROM AttendanceRecord a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.role
            LEFT JOIN FETCH a.schoolClass c
            LEFT JOIN FETCH c.school
            LEFT JOIN FETCH a.markedBy
            WHERE (:date IS NULL OR a.attendanceDate = :date)
              AND (:classUuid IS NULL OR c.uuid = :classUuid)
              AND (:userUuid IS NULL OR u.uuid = :userUuid)
            ORDER BY a.attendanceDate DESC, u.name
            """)
    List<AttendanceRecord> findFiltered(
            @Param("date") LocalDate date,
            @Param("classUuid") UUID classUuid,
            @Param("userUuid") UUID userUuid);

    Optional<AttendanceRecord> findByUser_UuidAndSchoolClass_UuidAndAttendanceDate(
            UUID userUuid, UUID classUuid, LocalDate date);

    Optional<AttendanceRecord> findByUser_UuidAndSchoolClassIsNullAndAttendanceDate(
            UUID userUuid, LocalDate date);
}
