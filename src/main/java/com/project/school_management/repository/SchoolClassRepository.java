package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.school_management.entities.SchoolClass;

@Repository
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

    @Query("""
            SELECT DISTINCT c FROM SchoolClass c
            JOIN FETCH c.school
            WHERE c.generation = :generation
            """)
    List<SchoolClass> findDetailedByGeneration(@Param("generation") Integer generation);

    @Query("""
            SELECT c FROM SchoolClass c
            JOIN FETCH c.school
            WHERE UPPER(c.joinCode) = UPPER(:joinCode)
            """)
    Optional<SchoolClass> findDetailedByJoinCode(@Param("joinCode") String joinCode);

    boolean existsByJoinCodeIgnoreCase(String joinCode);

    @Query("""
            SELECT DISTINCT c.generation FROM SchoolClass c
            WHERE c.generation IS NOT NULL
            ORDER BY c.generation
            """)
    List<Integer> findDistinctGenerations();
}
