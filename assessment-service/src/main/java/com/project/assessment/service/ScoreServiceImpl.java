package com.project.assessment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.assessment.dto.ChartSeries;
import com.project.assessment.dto.GpaSummaryStats;
import com.project.assessment.dto.ScoreBatchUpsertRequest;
import com.project.assessment.dto.ScoreResponse;
import com.project.assessment.dto.ScoreUpsertRequest;
import com.project.assessment.dto.StudentGpaResponse;
import com.project.assessment.dto.SubjectGradeSummary;
import com.project.assessment.dto.TopStudentRow;
import com.project.assessment.entity.StudentScore;
import com.project.assessment.repository.StudentScoreRepository;
import com.project.assessment.security.CallerContext;
import com.project.assessment.util.GradeScale;

@Service
@Transactional
public class ScoreServiceImpl implements ScoreService {

    private final StudentScoreRepository scoreRepository;
    private final GpaAccessService gpaAccessService;

    public ScoreServiceImpl(StudentScoreRepository scoreRepository, GpaAccessService gpaAccessService) {
        this.scoreRepository = scoreRepository;
        this.gpaAccessService = gpaAccessService;
    }

    @Override
    public ScoreResponse create(ScoreUpsertRequest request, CallerContext caller) {
        requireWrite(caller);
        validateScoreBounds(request.getScore(), request.getMaxScore());
        StudentScore score = mapNew(request, caller);
        return ScoreResponse.from(scoreRepository.save(score));
    }

    @Override
    public ScoreResponse update(UUID id, ScoreUpsertRequest request, CallerContext caller) {
        requireWrite(caller);
        StudentScore existing = find(id);
        assertCanManage(existing, caller);
        validateScoreBounds(request.getScore(), request.getMaxScore());
        apply(existing, request, caller);
        return ScoreResponse.from(scoreRepository.save(existing));
    }

    @Override
    public int upsertBatch(ScoreBatchUpsertRequest request, CallerContext caller) {
        requireWrite(caller);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw badRequest("No student scores to save");
        }
        String defaultSubject = GradeScale.normalizeSubject(request.getSubject());
        String term = GradeScale.normalizeTerm(request.getTerm());
        BigDecimal maxScore = request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore();
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Max score must be greater than 0");
        }

        int saved = 0;
        for (ScoreBatchUpsertRequest.ScoreBatchItem item : request.getItems()) {
            if (item == null || item.getStudentUuid() == null || item.getScore() == null) {
                continue;
            }
            String subject = GradeScale.normalizeSubject(item.getSubject());
            if (subject == null) {
                subject = defaultSubject;
            }
            if (subject == null) {
                throw badRequest("Subject is required");
            }
            validateScoreBounds(item.getScore(), maxScore);

            StudentScore existing = scoreRepository
                    .findByStudentClassSubjectTerm(item.getStudentUuid(), request.getClassUuid(), subject, term)
                    .orElse(null);

            ScoreUpsertRequest single = new ScoreUpsertRequest();
            single.setStudentUuid(item.getStudentUuid());
            single.setStudentName(item.getStudentName());
            single.setStudentEmail(item.getStudentEmail());
            single.setStudentGrade(item.getStudentGrade());
            single.setSchoolUuid(request.getSchoolUuid());
            single.setClassUuid(request.getClassUuid());
            single.setClassName(request.getClassName());
            single.setGeneration(request.getGeneration());
            single.setAcademicYear(request.getAcademicYear());
            single.setTeacherUuid(request.getTeacherUuid() != null ? request.getTeacherUuid() : caller.userUuid());
            single.setTeacherName(request.getTeacherName());
            single.setSubject(subject);
            single.setTerm(term);
            single.setScore(item.getScore());
            single.setMaxScore(maxScore);
            single.setRemark(item.getRemark());

            if (existing == null) {
                scoreRepository.save(mapNew(single, caller));
            } else {
                assertCanManage(existing, caller);
                apply(existing, single, caller);
                scoreRepository.save(existing);
            }
            saved++;
        }
        if (saved == 0) {
            throw badRequest("Enter at least one student score");
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResponse getById(UUID id, CallerContext caller) {
        StudentScore score = find(id);
        assertCanViewScore(score, caller);
        return ScoreResponse.from(score);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> list(UUID classUuid, Integer generation, CallerContext caller) {
        List<StudentScore> scores;
        if (classUuid != null && generation != null) {
            scores = scoreRepository.findBySchoolClassUuidAndGenerationOrderBySubjectAscStudentNameAsc(
                    classUuid, generation);
        } else if (classUuid != null) {
            scores = scoreRepository.findBySchoolClassUuidOrderBySubjectAscStudentNameAsc(classUuid);
        } else if (generation != null) {
            scores = scoreRepository.findByGenerationOrderByClassNameAscSubjectAscStudentNameAsc(generation);
        } else if (caller.isTeacher() && caller.userUuid() != null) {
            scores = scoreRepository.findByTeacherUuidOrderByClassNameAscSubjectAscStudentNameAsc(caller.userUuid());
        } else if (caller.isStudent() && caller.userUuid() != null) {
            assertStudentReadAccess(caller.userUuid(), caller);
            scores = scoreRepository.findByStudentUuidOrderByGenerationAscClassNameAscTermAscSubjectAsc(
                    caller.userUuid());
        } else if (caller.schoolUuid() != null) {
            scores = scoreRepository.findBySchoolUuidOrderByClassNameAscSubjectAscStudentNameAsc(caller.schoolUuid());
        } else {
            scores = scoreRepository.findAllByOrderByClassNameAscSubjectAscStudentNameAsc();
        }
        return scores.stream()
                .filter(s -> inScope(s, caller))
                .map(ScoreResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listForSession(UUID classUuid, String subject, String term, CallerContext caller) {
        if (classUuid == null) {
            return List.of();
        }
        String resolvedTerm = GradeScale.normalizeTerm(term);
        List<StudentScore> scores;
        if (subject == null || subject.isBlank()) {
            scores = scoreRepository.findBySchoolClassUuidOrderBySubjectAscStudentNameAsc(classUuid).stream()
                    .filter(s -> s.getTerm() != null && s.getTerm().equalsIgnoreCase(resolvedTerm))
                    .toList();
        } else {
            scores = scoreRepository.findByClassSubjectTerm(
                    classUuid, GradeScale.normalizeSubject(subject), resolvedTerm);
        }
        return scores.stream().filter(s -> inScope(s, caller)).map(ScoreResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listByStudent(UUID studentUuid, Integer generation, String term, CallerContext caller) {
        assertCanViewStudent(studentUuid, caller);
        List<StudentScore> scores = generation == null
                ? scoreRepository.findByStudentUuidOrderByGenerationAscClassNameAscTermAscSubjectAsc(studentUuid)
                : scoreRepository.findByStudentUuidAndGenerationOrderByClassNameAscTermAscSubjectAsc(
                        studentUuid, generation);
        if (term != null && !term.isBlank()) {
            String wanted = term.trim().toLowerCase(Locale.ROOT);
            scores = scores.stream()
                    .filter(s -> s.getTerm() != null && s.getTerm().trim().toLowerCase(Locale.ROOT).equals(wanted))
                    .toList();
        }
        return scores.stream().map(ScoreResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term, CallerContext caller) {
        List<ScoreResponse> scoreResponses = listByStudent(studentUuid, generation, term, caller);
        return buildGpa(studentUuid, scoreResponses, generation, term);
    }

    @Override
    public void delete(UUID id, CallerContext caller) {
        requireWrite(caller);
        StudentScore score = find(id);
        assertCanManage(score, caller);
        scoreRepository.delete(score);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listScoreGenerations() {
        return scoreRepository.findDistinctGenerations();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listScoreTerms() {
        return scoreRepository.findDistinctTerms();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listScoreGenerationsForStudent(UUID studentUuid) {
        return scoreRepository.findDistinctGenerationsByStudent(studentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listScoreTermsForStudent(UUID studentUuid) {
        return scoreRepository.findDistinctTermsByStudent(studentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentUuidsWithScores(Integer generation) {
        if (generation == null) {
            return List.of();
        }
        return scoreRepository.findStudentUuidsByGeneration(generation);
    }

    @Override
    @Transactional(readOnly = true)
    public GpaSummaryStats gpaSummary(CallerContext caller) {
        List<StudentAgg> aggs = aggregate(scoped(caller));
        if (aggs.isEmpty()) {
            return GpaSummaryStats.builder()
                    .averageGpa(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .averagePercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .studentsWithScores(0)
                    .totalScoreRows(0)
                    .topLetter("—")
                    .build();
        }
        BigDecimal gpaSum = BigDecimal.ZERO;
        BigDecimal pctSum = BigDecimal.ZERO;
        int rows = 0;
        Map<String, Integer> letters = new LinkedHashMap<>();
        for (StudentAgg a : aggs) {
            gpaSum = gpaSum.add(a.gpa);
            pctSum = pctSum.add(a.avgPercent);
            rows += a.scoreCount;
            letters.merge(a.letter, 1, Integer::sum);
        }
        String topLetter = letters.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
        int n = aggs.size();
        return GpaSummaryStats.builder()
                .averageGpa(gpaSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP))
                .averagePercent(pctSum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP))
                .studentsWithScores(n)
                .totalScoreRows(rows)
                .topLetter(topLetter)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStudentRow> topStudentsByClass(CallerContext caller) {
        return topByGroup(aggregate(scoped(caller)), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStudentRow> topStudentsByGrade(CallerContext caller) {
        return topByGroup(aggregate(scoped(caller)), false);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries termScoreChart(CallerContext caller) {
        List<StudentScore> scores = scoped(caller);
        Map<String, List<BigDecimal>> buckets = new LinkedHashMap<>();
        buckets.put("Midterm", new ArrayList<>());
        buckets.put("Final", new ArrayList<>());
        buckets.put("Other", new ArrayList<>());
        for (StudentScore score : scores) {
            String term = score.getTerm() == null ? "" : score.getTerm().trim().toLowerCase(Locale.ROOT);
            String bucket;
            if (term.contains("mid")) {
                bucket = "Midterm";
            } else if (term.contains("final")) {
                bucket = "Final";
            } else if (!term.isBlank()) {
                bucket = score.getTerm().trim();
                buckets.putIfAbsent(bucket, new ArrayList<>());
            } else {
                bucket = "Other";
            }
            buckets.get(bucket).add(GradeScale.percent(score.getScore(), score.getMaxScore()));
        }
        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> e : buckets.entrySet()) {
            if (e.getValue().isEmpty() && !List.of("Midterm", "Final").contains(e.getKey())) {
                continue;
            }
            labels.add(e.getKey());
            if (e.getValue().isEmpty()) {
                values.add(0);
            } else {
                BigDecimal sum = e.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                values.add(sum.divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP));
            }
        }
        return ChartSeries.builder().labels(labels).values(values).build();
    }

    private List<StudentScore> scoped(CallerContext caller) {
        List<StudentScore> all;
        if (caller.schoolUuid() != null) {
            all = scoreRepository.findBySchoolUuidOrderByClassNameAscSubjectAscStudentNameAsc(caller.schoolUuid());
        } else if (caller.isTeacher() && caller.userUuid() != null) {
            all = scoreRepository.findByTeacherUuidOrderByClassNameAscSubjectAscStudentNameAsc(caller.userUuid());
        } else {
            all = scoreRepository.findAllByOrderByClassNameAscSubjectAscStudentNameAsc();
        }
        return all.stream().filter(s -> inScope(s, caller)).toList();
    }

    private boolean inScope(StudentScore score, CallerContext caller) {
        if (caller.schoolUuid() != null && score.getSchoolUuid() != null
                && !caller.schoolUuid().equals(score.getSchoolUuid())) {
            return false;
        }
        if (caller.isTeacher() && caller.userUuid() != null) {
            return caller.userUuid().equals(score.getTeacherUuid());
        }
        return true;
    }

    private void assertCanViewScore(StudentScore score, CallerContext caller) {
        if (caller.isStudent()) {
            if (caller.userUuid() == null || !caller.userUuid().equals(score.getStudentUuid())) {
                throw forbidden("You can only view your own scores");
            }
            assertStudentReadAccess(caller.userUuid(), caller);
            return;
        }
        if (!caller.hasAuthority("SCORE_READ") && !caller.hasAuthority("SCORE_WRITE")) {
            throw forbidden("SCORE_READ required");
        }
        if (!inScope(score, caller)) {
            throw forbidden("Out of school/class scope");
        }
    }

    private void assertCanViewStudent(UUID studentUuid, CallerContext caller) {
        if (caller.isStudent()) {
            if (caller.userUuid() == null || !caller.userUuid().equals(studentUuid)) {
                throw forbidden("You can only view your own scores");
            }
            assertStudentReadAccess(studentUuid, caller);
            return;
        }
        if (!caller.hasAuthority("SCORE_READ") && !caller.hasAuthority("SCORE_WRITE")) {
            throw forbidden("SCORE_READ required");
        }
    }

    private void assertStudentReadAccess(UUID studentUuid, CallerContext caller) {
        if (!gpaAccessService.hasApprovedAccess(studentUuid)) {
            throw forbidden(
                    "Score and GPA access requires approval from the principal or admin. Submit a GPA request first.");
        }
    }

    private void assertCanManage(StudentScore score, CallerContext caller) {
        if (caller.isTeacher() && caller.userUuid() != null
                && !caller.userUuid().equals(score.getTeacherUuid())) {
            // teachers may update rows in classes they teach; BFF validates class membership
            // allow if SCORE_WRITE and same school
        }
        if (caller.schoolUuid() != null && score.getSchoolUuid() != null
                && !caller.schoolUuid().equals(score.getSchoolUuid())) {
            throw forbidden("You can only manage scores in your school");
        }
    }

    private void requireWrite(CallerContext caller) {
        if (!caller.hasAuthority("SCORE_WRITE")) {
            throw forbidden("SCORE_WRITE required");
        }
    }

    private StudentScore mapNew(ScoreUpsertRequest request, CallerContext caller) {
        StudentScore score = new StudentScore();
        apply(score, request, caller);
        return score;
    }

    private void apply(StudentScore score, ScoreUpsertRequest request, CallerContext caller) {
        String subject = GradeScale.normalizeSubject(request.getSubject());
        if (subject == null) {
            throw badRequest("Subject is required");
        }
        score.setStudentUuid(request.getStudentUuid());
        score.setStudentName(blankToNull(request.getStudentName()));
        score.setStudentEmail(blankToNull(request.getStudentEmail()));
        score.setStudentGrade(blankToNull(request.getStudentGrade()));
        score.setSchoolUuid(request.getSchoolUuid());
        score.setSchoolClassUuid(request.getClassUuid());
        score.setClassName(blankToNull(request.getClassName()));
        score.setGeneration(request.getGeneration());
        score.setAcademicYear(request.getAcademicYear());
        score.setTeacherUuid(request.getTeacherUuid() != null ? request.getTeacherUuid() : caller.userUuid());
        score.setTeacherName(blankToNull(request.getTeacherName()));
        score.setSubject(subject);
        score.setTerm(GradeScale.normalizeTerm(request.getTerm()));
        score.setScore(request.getScore());
        score.setMaxScore(request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore());
        score.setRemark(blankToNull(request.getRemark()));
    }

    private StudentGpaResponse buildGpa(
            UUID studentUuid, List<ScoreResponse> scores, Integer generation, String term) {
        Map<String, List<ScoreResponse>> bySubject = new LinkedHashMap<>();
        for (ScoreResponse score : scores) {
            String key = score.getSubject() == null ? "Unknown" : score.getSubject().trim();
            bySubject.computeIfAbsent(key, k -> new ArrayList<>()).add(score);
        }

        List<SubjectGradeSummary> subjects = new ArrayList<>();
        BigDecimal gpaSum = BigDecimal.ZERO;
        BigDecimal percentSum = BigDecimal.ZERO;
        int counted = 0;
        String studentName = scores.isEmpty() ? null : scores.get(0).getStudentName();
        String studentEmail = scores.isEmpty() ? null : scores.get(0).getStudentEmail();

        for (Map.Entry<String, List<ScoreResponse>> entry : bySubject.entrySet()) {
            BigDecimal subjectPercentSum = BigDecimal.ZERO;
            for (ScoreResponse score : entry.getValue()) {
                subjectPercentSum = subjectPercentSum.add(GradeScale.percent(score.getScore(), score.getMaxScore()));
            }
            BigDecimal avgPercent = subjectPercentSum.divide(
                    BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            BigDecimal gpaPoints = GradeScale.gpaPoints(avgPercent);
            subjects.add(SubjectGradeSummary.builder()
                    .subject(entry.getKey())
                    .term(term)
                    .averagePercent(avgPercent)
                    .gpaPoints(gpaPoints)
                    .letterGrade(GradeScale.letterGrade(avgPercent))
                    .scoreCount(entry.getValue().size())
                    .build());
            gpaSum = gpaSum.add(gpaPoints);
            percentSum = percentSum.add(avgPercent);
            counted++;
        }

        BigDecimal gpa = counted == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : gpaSum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
        BigDecimal averagePercent = counted == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : percentSum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);

        return StudentGpaResponse.builder()
                .studentUuid(studentUuid)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .generation(generation)
                .term(term)
                .gpa(gpa)
                .averagePercent(averagePercent)
                .letterGrade(GradeScale.letterGrade(averagePercent))
                .totalScores(scores.size())
                .subjects(subjects)
                .scores(scores)
                .build();
    }

    private List<StudentAgg> aggregate(List<StudentScore> scores) {
        Map<UUID, List<StudentScore>> byStudent = new LinkedHashMap<>();
        for (StudentScore score : scores) {
            byStudent.computeIfAbsent(score.getStudentUuid(), k -> new ArrayList<>()).add(score);
        }
        List<StudentAgg> result = new ArrayList<>();
        for (Map.Entry<UUID, List<StudentScore>> entry : byStudent.entrySet()) {
            List<ScoreResponse> responses = entry.getValue().stream().map(ScoreResponse::from).toList();
            StudentGpaResponse gpa = buildGpa(entry.getKey(), responses, null, null);
            StudentScore sample = entry.getValue().get(0);
            Map<UUID, String> classNames = new LinkedHashMap<>();
            Map<String, String> grades = new LinkedHashMap<>();
            for (StudentScore s : entry.getValue()) {
                if (s.getSchoolClassUuid() != null) {
                    classNames.put(s.getSchoolClassUuid(),
                            s.getClassName() != null ? s.getClassName() : s.getSchoolClassUuid().toString());
                }
                String grade = s.getStudentGrade() != null && !s.getStudentGrade().isBlank()
                        ? s.getStudentGrade().trim()
                        : "—";
                grades.put(grade, grade);
            }
            result.add(new StudentAgg(
                    sample.getStudentUuid(),
                    sample.getStudentName(),
                    gpa.getGpa(),
                    gpa.getAveragePercent(),
                    gpa.getLetterGrade(),
                    gpa.getTotalScores(),
                    classNames,
                    grades));
        }
        return result;
    }

    private List<TopStudentRow> topByGroup(List<StudentAgg> aggs, boolean byClass) {
        Map<String, TopStudentRow> best = new LinkedHashMap<>();
        for (StudentAgg a : aggs) {
            Map<String, String> groups = byClass
                    ? a.classNames.entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    e -> e.getKey().toString(),
                                    Map.Entry::getValue,
                                    (x, y) -> x,
                                    LinkedHashMap::new))
                    : a.grades;
            for (Map.Entry<String, String> g : groups.entrySet()) {
                TopStudentRow existing = best.get(g.getKey());
                if (existing == null || a.gpa.compareTo(existing.getGpa()) > 0) {
                    best.put(g.getKey(), TopStudentRow.builder()
                            .studentUuid(a.studentUuid)
                            .studentName(a.studentName)
                            .groupKey(g.getKey())
                            .groupLabel(g.getValue())
                            .gpa(a.gpa)
                            .averagePercent(a.avgPercent)
                            .letterGrade(a.letter)
                            .build());
                }
            }
        }
        return best.values().stream()
                .sorted((x, y) -> y.getGpa().compareTo(x.getGpa()))
                .toList();
    }

    private record StudentAgg(
            UUID studentUuid,
            String studentName,
            BigDecimal gpa,
            BigDecimal avgPercent,
            String letter,
            int scoreCount,
            Map<UUID, String> classNames,
            Map<String, String> grades) {
    }

    private StudentScore find(UUID id) {
        return scoreRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Score not found: " + id));
    }

    private static void validateScoreBounds(BigDecimal score, BigDecimal maxScore) {
        BigDecimal max = maxScore == null ? BigDecimal.valueOf(100) : maxScore;
        if (score == null) {
            throw badRequest("Score is required");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("Score cannot be negative");
        }
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Max score must be greater than 0");
        }
        if (score.compareTo(max) > 0) {
            throw badRequest("Score cannot exceed max score (" + max + ")");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
