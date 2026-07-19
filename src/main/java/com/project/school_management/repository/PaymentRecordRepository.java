package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.PaymentRecord;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    @Query("""
            SELECT DISTINCT p FROM PaymentRecord p
            JOIN FETCH p.user u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH p.createdBy
            WHERE p.uuid = :id
            """)
    Optional<PaymentRecord> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM PaymentRecord p
            JOIN FETCH p.user u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH p.createdBy
            WHERE (:userUuid IS NULL OR u.uuid = :userUuid)
            ORDER BY p.createdAt DESC
            """)
    List<PaymentRecord> findDetailed(@Param("userUuid") UUID userUuid);
}
