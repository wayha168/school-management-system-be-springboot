package com.project.school_management.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.StudentScore;

public interface StudentScoreRepository extends JpaRepository<StudentScore, UUID> {

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE s.uuid = :id
            """)
    Optional<StudentScore> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE c.uuid = :classUuid
            ORDER BY s.subject, s.student.name
            """)
    List<StudentScore> findDetailedByClassUuid(@Param("classUuid") UUID classUuid);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE c.uuid = :classUuid AND c.generation = :generation
            ORDER BY s.subject, s.student.name
            """)
    List<StudentScore> findDetailedByClassUuidAndGeneration(
            @Param("classUuid") UUID classUuid,
            @Param("generation") Integer generation);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE c.generation = :generation
            ORDER BY c.name, s.subject, s.student.name
            """)
    List<StudentScore> findDetailedByGeneration(@Param("generation") Integer generation);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE s.teacher.uuid = :teacherUuid
            ORDER BY c.name, s.subject, s.student.name
            """)
    List<StudentScore> findDetailedByTeacherUuid(@Param("teacherUuid") UUID teacherUuid);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            ORDER BY c.name, s.subject, s.student.name
            """)
    List<StudentScore> findAllDetailed();

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student st
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE st.uuid = :studentUuid
            ORDER BY c.generation, c.name, s.term, s.subject
            """)
    List<StudentScore> findDetailedByStudentUuid(@Param("studentUuid") UUID studentUuid);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student st
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE st.uuid = :studentUuid AND c.generation = :generation
            ORDER BY c.name, s.term, s.subject
            """)
    List<StudentScore> findDetailedByStudentUuidAndGeneration(
            @Param("studentUuid") UUID studentUuid,
            @Param("generation") Integer generation);

    @Query("""
            SELECT DISTINCT c.generation FROM StudentScore s
            JOIN s.schoolClass c
            WHERE c.generation IS NOT NULL
            ORDER BY c.generation
            """)
    List<Integer> findDistinctGenerations();

    @Query("""
            SELECT DISTINCT s.term FROM StudentScore s
            WHERE s.term IS NOT NULL AND s.term <> ''
            ORDER BY s.term
            """)
    List<String> findDistinctTerms();

    @Query("""
            SELECT DISTINCT c.generation FROM StudentScore s
            JOIN s.schoolClass c
            JOIN s.student st
            WHERE st.uuid = :studentUuid AND c.generation IS NOT NULL
            ORDER BY c.generation
            """)
    List<Integer> findDistinctGenerationsByStudent(@Param("studentUuid") UUID studentUuid);

    @Query("""
            SELECT DISTINCT s.term FROM StudentScore s
            JOIN s.student st
            WHERE st.uuid = :studentUuid AND s.term IS NOT NULL AND s.term <> ''
            ORDER BY s.term
            """)
    List<String> findDistinctTermsByStudent(@Param("studentUuid") UUID studentUuid);

    @Query("""
            SELECT DISTINCT st.uuid FROM StudentScore s
            JOIN s.student st
            JOIN s.schoolClass c
            WHERE c.generation = :generation
            """)
    List<UUID> findStudentUuidsByGeneration(@Param("generation") Integer generation);

    @Query("""
            SELECT DISTINCT s FROM StudentScore s
            JOIN FETCH s.student
            JOIN FETCH s.schoolClass c
            JOIN FETCH c.school
            JOIN FETCH s.teacher
            WHERE c.uuid = :classUuid
              AND LOWER(s.subject) = LOWER(:subject)
              AND LOWER(s.term) = LOWER(:term)
            ORDER BY s.student.name
            """)
    List<StudentScore> findDetailedByClassSubjectTerm(
            @Param("classUuid") UUID classUuid,
            @Param("subject") String subject,
            @Param("term") String term);

    @Query("""
            SELECT s FROM StudentScore s
            WHERE s.student.uuid = :studentUuid
              AND s.schoolClass.uuid = :classUuid
              AND LOWER(s.subject) = LOWER(:subject)
              AND LOWER(s.term) = LOWER(:term)
            """)
    Optional<StudentScore> findByStudentClassSubjectTerm(
            @Param("studentUuid") UUID studentUuid,
            @Param("classUuid") UUID classUuid,
            @Param("subject") String subject,
            @Param("term") String term);
}
