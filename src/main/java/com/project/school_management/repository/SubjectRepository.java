package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.school_management.entities.Subject;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    @Query("""
            SELECT s FROM Subject s
            JOIN FETCH s.school
            WHERE s.uuid = :id
            """)
    Optional<Subject> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Subject s
            JOIN FETCH s.school
            ORDER BY s.name
            """)
    List<Subject> findAllDetailed();

    @Query("""
            SELECT s FROM Subject s
            JOIN FETCH s.school
            WHERE s.school.uuid = :schoolUuid
            ORDER BY s.name
            """)
    List<Subject> findDetailedBySchoolUuid(@Param("schoolUuid") UUID schoolUuid);

    boolean existsBySchool_UuidAndNameIgnoreCase(UUID schoolUuid, String name);

    boolean existsBySchool_UuidAndCodeIgnoreCase(UUID schoolUuid, String code);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Subject s
            WHERE s.school.uuid = :schoolUuid
              AND LOWER(s.name) = LOWER(:name)
              AND s.uuid <> :excludeId
            """)
    boolean existsBySchoolAndNameExcluding(
            @Param("schoolUuid") UUID schoolUuid,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId);
}
