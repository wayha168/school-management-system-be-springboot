package com.project.assessment.service;

import java.util.List;
import java.util.UUID;

import com.project.assessment.dto.ChartSeries;
import com.project.assessment.dto.GpaSummaryStats;
import com.project.assessment.dto.ScoreBatchUpsertRequest;
import com.project.assessment.dto.ScoreResponse;
import com.project.assessment.dto.ScoreUpsertRequest;
import com.project.assessment.dto.StudentGpaResponse;
import com.project.assessment.dto.TopStudentRow;
import com.project.assessment.security.CallerContext;

public interface ScoreService {

    ScoreResponse create(ScoreUpsertRequest request, CallerContext caller);

    ScoreResponse update(UUID id, ScoreUpsertRequest request, CallerContext caller);

    int upsertBatch(ScoreBatchUpsertRequest request, CallerContext caller);

    ScoreResponse getById(UUID id, CallerContext caller);

    List<ScoreResponse> list(UUID classUuid, Integer generation, CallerContext caller);

    List<ScoreResponse> listForSession(UUID classUuid, String subject, String term, CallerContext caller);

    List<ScoreResponse> listByStudent(UUID studentUuid, Integer generation, String term, CallerContext caller);

    StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term, CallerContext caller);

    void delete(UUID id, CallerContext caller);

    List<Integer> listScoreGenerations();

    List<String> listScoreTerms();

    List<Integer> listScoreGenerationsForStudent(UUID studentUuid);

    List<String> listScoreTermsForStudent(UUID studentUuid);

    List<UUID> listStudentUuidsWithScores(Integer generation);

    GpaSummaryStats gpaSummary(CallerContext caller);

    List<TopStudentRow> topStudentsByClass(CallerContext caller);

    List<TopStudentRow> topStudentsByGrade(CallerContext caller);

    ChartSeries termScoreChart(CallerContext caller);
}
