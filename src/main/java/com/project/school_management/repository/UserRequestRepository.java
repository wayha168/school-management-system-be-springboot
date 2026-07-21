package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.UserRequest;
import com.project.school_management.enums.RequestCategory;
import com.project.school_management.enums.RequestStatus;

public interface UserRequestRepository extends JpaRepository<UserRequest, UUID> {

    @Query("""
            SELECT DISTINCT r FROM UserRequest r
            JOIN FETCH r.fromUser u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH r.handledBy
            WHERE r.uuid = :id
            """)
    Optional<UserRequest> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT r FROM UserRequest r
            JOIN FETCH r.fromUser u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH r.handledBy
            WHERE u.uuid = :userUuid
            ORDER BY r.createdAt DESC
            """)
    List<UserRequest> findDetailedByFromUser(@Param("userUuid") UUID userUuid);

    @Query("""
            SELECT DISTINCT r FROM UserRequest r
            JOIN FETCH r.fromUser u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH r.handledBy
            WHERE (:filterStatus = false OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    List<UserRequest> findDetailedByStatus(
            @Param("filterStatus") boolean filterStatus,
            @Param("status") RequestStatus status);

    @Query("""
            SELECT DISTINCT r FROM UserRequest r
            JOIN FETCH r.fromUser u
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH r.handledBy
            WHERE u.uuid = :userUuid
              AND r.category = :category
            ORDER BY r.createdAt DESC
            """)
    List<UserRequest> findDetailedByFromUserAndCategory(
            @Param("userUuid") UUID userUuid,
            @Param("category") RequestCategory category);

    boolean existsByFromUser_UuidAndCategoryAndStatus(
            UUID fromUserUuid,
            RequestCategory category,
            RequestStatus status);
}
