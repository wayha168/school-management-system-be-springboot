package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.SchoolClass;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    @Query("""
            SELECT c FROM SchoolClass c
            JOIN FETCH c.school
            WHERE c.uuid = :id
            """)
    Optional<SchoolClass> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT c FROM SchoolClass c
            JOIN FETCH c.school
            """)
    List<SchoolClass> findAllDetailed();

    @Query("""
            SELECT c FROM SchoolClass c
            JOIN FETCH c.school
            WHERE c.school.uuid = :schoolUuid
            """)
    List<SchoolClass> findDetailedBySchoolUuid(@Param("schoolUuid") UUID schoolUuid);
}
