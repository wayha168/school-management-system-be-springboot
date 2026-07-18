package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role
            JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClass
            WHERE u.uuid = :id
            """)
    Optional<User> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.role
            JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClass
            """)
    List<User> findAllDetailed();

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role
            JOIN FETCH u.school
            LEFT JOIN FETCH u.schoolClass
            WHERE u.email = :email
            """)
    Optional<User> findDetailedByEmail(@Param("email") String email);
}
