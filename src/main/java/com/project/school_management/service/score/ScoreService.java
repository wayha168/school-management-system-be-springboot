package com.project.school_management.service.score;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.dto.dashboard.TopStudentRow;
import com.project.school_management.dto.score.ScoreBatchRequest;
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.score.StudentGpaResponse;

public interface ScoreService {

    ScoreResponse create(ScoreRequest request);

    ScoreResponse update(UUID id, ScoreRequest request);

    /** Create or update scores for many students in one class/subject/term session. */
    int upsertBatch(ScoreBatchRequest request);

    /** Existing scores for a class + subject + term (to prefill the session form). */
    List<ScoreResponse> listForSession(UUID classUuid, String subject, String term);

    ScoreResponse getById(UUID id);

    List<ScoreResponse> list(UUID classUuid, Integer generation);

    List<ScoreResponse> listByStudent(UUID studentUuid, Integer generation, String term);

    StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term);

    StudentGpaResponse getMyGpa(Integer generation, String term);

    List<ScoreResponse> listMyScores(Integer generation, String term);

    void delete(UUID id);

    byte[] exportExcel(UUID classUuid, Integer generation);

    int importExcel(MultipartFile file, UUID classUuid);

    /** Generations that have score rows (sessions involving grades). */
    List<Integer> listScoreGenerations();

    /** Terms that have score rows. */
    List<String> listScoreTerms();

    List<Integer> listScoreGenerationsForStudent(UUID studentUuid);

    List<String> listScoreTermsForStudent(UUID studentUuid);

    /** Student UUIDs that have scores in the given generation. */
    List<UUID> listStudentUuidsWithScores(Integer generation);

    GpaSummaryStats gpaSummary();

    List<TopStudentRow> topStudentsByClass();

    List<TopStudentRow> topStudentsByGrade();

    /** Average % by term bucket (Midterm / Final / other terms). */
    ChartSeries termScoreChart();
}
