package com.project.assessment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.assessment.entity.StudentScore;

public interface StudentScoreRepository extends JpaRepository<StudentScore, UUID> {

    Optional<StudentScore> findByUuid(UUID uuid);

    @Query("""
            SELECT s FROM StudentScore s
            WHERE s.studentUuid = :studentUuid
              AND s.schoolClassUuid = :classUuid
              AND LOWER(s.subject) = LOWER(:subject)
              AND LOWER(s.term) = LOWER(:term)
            """)
    Optional<StudentScore> findByStudentClassSubjectTerm(
            @Param("studentUuid") UUID studentUuid,
            @Param("classUuid") UUID classUuid,
            @Param("subject") String subject,
            @Param("term") String term);

    List<StudentScore> findBySchoolClassUuidOrderBySubjectAscStudentNameAsc(UUID classUuid);

    List<StudentScore> findBySchoolClassUuidAndGenerationOrderBySubjectAscStudentNameAsc(
            UUID classUuid, Integer generation);

    List<StudentScore> findByGenerationOrderByClassNameAscSubjectAscStudentNameAsc(Integer generation);

    List<StudentScore> findByTeacherUuidOrderByClassNameAscSubjectAscStudentNameAsc(UUID teacherUuid);

    List<StudentScore> findByStudentUuidOrderByGenerationAscClassNameAscTermAscSubjectAsc(UUID studentUuid);

    List<StudentScore> findByStudentUuidAndGenerationOrderByClassNameAscTermAscSubjectAsc(
            UUID studentUuid, Integer generation);

    List<StudentScore> findBySchoolUuidOrderByClassNameAscSubjectAscStudentNameAsc(UUID schoolUuid);

    List<StudentScore> findAllByOrderByClassNameAscSubjectAscStudentNameAsc();

    @Query("""
            SELECT s FROM StudentScore s
            WHERE s.schoolClassUuid = :classUuid
              AND LOWER(s.subject) = LOWER(:subject)
              AND LOWER(s.term) = LOWER(:term)
            ORDER BY s.studentName
            """)
    List<StudentScore> findByClassSubjectTerm(
            @Param("classUuid") UUID classUuid,
            @Param("subject") String subject,
            @Param("term") String term);

    @Query("SELECT DISTINCT s.generation FROM StudentScore s WHERE s.generation IS NOT NULL ORDER BY s.generation")
    List<Integer> findDistinctGenerations();

    @Query("SELECT DISTINCT s.term FROM StudentScore s WHERE s.term IS NOT NULL AND s.term <> '' ORDER BY s.term")
    List<String> findDistinctTerms();

    @Query("""
            SELECT DISTINCT s.generation FROM StudentScore s
            WHERE s.studentUuid = :studentUuid AND s.generation IS NOT NULL
            ORDER BY s.generation
            """)
    List<Integer> findDistinctGenerationsByStudent(@Param("studentUuid") UUID studentUuid);

    @Query("""
            SELECT DISTINCT s.term FROM StudentScore s
            WHERE s.studentUuid = :studentUuid AND s.term IS NOT NULL AND s.term <> ''
            ORDER BY s.term
            """)
    List<String> findDistinctTermsByStudent(@Param("studentUuid") UUID studentUuid);

    @Query("SELECT DISTINCT s.studentUuid FROM StudentScore s WHERE s.generation = :generation")
    List<UUID> findStudentUuidsByGeneration(@Param("generation") Integer generation);
}
