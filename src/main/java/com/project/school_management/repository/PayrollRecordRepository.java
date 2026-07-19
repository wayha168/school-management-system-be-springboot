package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.PayrollRecord;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, UUID> {

    @Query("""
            SELECT DISTINCT p FROM PayrollRecord p
            JOIN FETCH p.user u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH p.createdBy
            WHERE p.uuid = :id
            """)
    Optional<PayrollRecord> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM PayrollRecord p
            JOIN FETCH p.user u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH p.createdBy
            WHERE (:userUuid IS NULL OR u.uuid = :userUuid)
            ORDER BY p.period DESC
            """)
    List<PayrollRecord> findDetailed(@Param("userUuid") UUID userUuid);
}
