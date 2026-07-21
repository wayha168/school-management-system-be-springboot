package com.project.school_management.service.score;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.attendance.ClassAttendanceOverview;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.dto.dashboard.TopStudentRow;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.score.ScoreBatchRequest;
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserResponse;

public interface ScoreService {

    ScoreResponse create(ScoreRequest request);

    ScoreResponse update(UUID id, ScoreRequest request);

    /** Create or update scores for many students in one class/subject/term session. */
    int upsertBatch(ScoreBatchRequest request);

    /**
     * Save class score-sheet entries (one score per student) for a period.
     * Period {@code monthly} always resolves to the real current calendar month.
     */
    int upsertClassSession(
            UUID classUuid,
            String period,
            String subject,
            BigDecimal maxScore,
            List<UUID> studentUuids,
            List<String> entryScores);

    /** Existing scores for a class + subject + term (to prefill the session form). */
    List<ScoreResponse> listForSession(UUID classUuid, String subject, String term);

    /** Prefill map: studentUuid → score for one class/subject/term session. */
    Map<UUID, ScoreResponse> scoresByStudentForSession(UUID classUuid, String subject, String term);

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

    // ── Class score-session helpers (view/BFF) ───────────────────────────────

    /** Classes the caller may open for score entry (teachers → assigned only). */
    List<SchoolClassResponse> visibleClassesForScoring(DataUser account, Integer generation);

    /** Class table rows: teachers + student counts. */
    List<ClassAttendanceOverview> buildClassOverviews(List<SchoolClassResponse> classes);

    List<UserResponse> studentsInClass(UUID classUuid);

    boolean canEnterScores(DataUser account);

    /** Normalize UI period key: monthly | term1 | term2 | midterm | final. */
    String normalizePeriodKey(String period);

    /** UI period → stored term label. Monthly uses real {@code YearMonth.now()}. */
    String resolveTermFromPeriod(String periodKey);

    /** Form term value → stored term label (e.g. "Monthly" → "Monthly 2026-07"). */
    String resolveTermFromForm(String term);

    /** Display label for the current calendar month (e.g. "July 2026"). */
    String currentMonthlyLabel();

    /** Pick subject from class list (default first when blank). */
    String resolveSubject(List<String> classSubjects, String requestedSubject);
}
