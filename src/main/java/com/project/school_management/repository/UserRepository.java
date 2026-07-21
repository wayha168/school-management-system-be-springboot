package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;

public interface UserRepository extends JpaRepository<User, UUID> {

        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        long countByRole_Name(RoleName roleName);

        long countBySchool_UuidAndRole_Name(UUID schoolUuid, RoleName roleName);

        long countBySchool_Uuid(UUID schoolUuid);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            WHERE u.uuid = :id
            """)
    Optional<User> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            """)
    List<User> findAllDetailed();

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            WHERE u.school.uuid = :schoolUuid
            """)
    List<User> findDetailedBySchoolUuid(@Param("schoolUuid") UUID schoolUuid);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findDetailedByEmail(@Param("email") String email);

        @Query("""
                        SELECT u FROM User u
                        JOIN FETCH u.role
                        WHERE LOWER(u.email) = LOWER(:email)
                        """)
        Optional<User> findForLogin(@Param("email") String email);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            WHERE u.role.name = :role
            """)
    List<User> findDetailedByRole(@Param("role") RoleName role);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClasses
            WHERE u.role.name = :role
              AND u.school.uuid = :schoolUuid
            """)
    List<User> findDetailedBySchoolUuidAndRole(
            @Param("schoolUuid") UUID schoolUuid,
            @Param("role") RoleName role);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.school
            JOIN FETCH u.schoolClasses sc
            WHERE sc.uuid = :classUuid
              AND u.role.name = :role
            """)
    List<User> findDetailedByClassAndRole(
            @Param("classUuid") UUID classUuid,
            @Param("role") RoleName role);
}
