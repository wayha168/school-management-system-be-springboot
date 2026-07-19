package com.project.school_management.service.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.dto.dashboard.TopStudentRow;
import com.project.school_management.dto.score.ScoreBatchRequest;
import com.project.school_management.dto.score.ScoreMarkItem;
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.dto.score.SubjectGradeSummary;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.StudentScore;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.StudentScoreRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.util.GradeScale;

@Service
@Transactional
public class ScoreServiceImpl implements ScoreService {

    private final StudentScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolScopeService schoolScopeService;
    private final ObjectProvider<ScoreExcelService> excelService;

    public ScoreServiceImpl(
            StudentScoreRepository scoreRepository,
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolScopeService schoolScopeService,
            ObjectProvider<ScoreExcelService> excelService) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolScopeService = schoolScopeService;
        this.excelService = excelService;
    }

    @Override
    public ScoreResponse create(ScoreRequest request) {
        StudentScore score = map(new StudentScore(), request);
        return ScoreResponse.from(scoreRepository.save(score));
    }

    @Override
    public ScoreResponse update(UUID id, ScoreRequest request) {
        StudentScore score = findScore(id);
        assertCanManage(score.getSchoolClass());
        map(score, request);
        return ScoreResponse.from(scoreRepository.save(score));
    }

    @Override
    public int upsertBatch(ScoreBatchRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No student scores to save");
        }
        String defaultSubject = blankToNull(request.getSubject());
        String term = blankTo(request.getTerm(), "Term 1");
        BigDecimal maxScore = request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore();
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max score must be greater than 0");
        }

        SchoolClass schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
        assertCanManage(schoolClass);

        int saved = 0;
        for (ScoreMarkItem item : request.getItems()) {
            if (item == null || item.getStudentUuid() == null || item.getScore() == null) {
                continue;
            }
            String subject = blankToNull(item.getSubject());
            if (subject == null) {
                subject = defaultSubject;
            }
            if (subject == null) {
                throw new IllegalArgumentException("Subject is required");
            }
            if (item.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Score cannot be negative");
            }
            if (item.getScore().compareTo(maxScore) > 0) {
                throw new IllegalArgumentException("Score cannot exceed max score (" + maxScore + ")");
            }

            StudentScore existing = scoreRepository
                    .findByStudentClassSubjectTerm(item.getStudentUuid(), schoolClass.getUuid(), subject, term)
                    .orElse(null);

            ScoreRequest single = new ScoreRequest();
            single.setStudentUuid(item.getStudentUuid());
            single.setClassUuid(schoolClass.getUuid());
            single.setSubject(subject);
            single.setTerm(term);
            single.setScore(item.getScore());
            single.setMaxScore(maxScore);
            single.setRemark(item.getRemark());

            if (existing == null) {
                scoreRepository.save(map(new StudentScore(), single));
            } else {
                assertCanManage(existing.getSchoolClass());
                scoreRepository.save(map(existing, single));
            }
            saved++;
        }
        if (saved == 0) {
            throw new IllegalArgumentException("Enter at least one student score");
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listForSession(UUID classUuid, String subject, String term) {
        if (classUuid == null) {
            return List.of();
        }
        String resolvedTerm = blankTo(term, "Term 1");
        if (subject == null || subject.isBlank()) {
            return scoreRepository.findDetailedByClassUuid(classUuid).stream()
                    .filter(s -> s.getTerm() != null
                            && s.getTerm().trim().equalsIgnoreCase(resolvedTerm))
                    .filter(this::scoreInScope)
                    .map(ScoreResponse::from)
                    .toList();
        }
        return scoreRepository
                .findDetailedByClassSubjectTerm(classUuid, subject.trim(), resolvedTerm)
                .stream()
                .filter(this::scoreInScope)
                .map(ScoreResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResponse getById(UUID id) {
        StudentScore score = findScore(id);
        if (score.getSchoolClass() != null && score.getSchoolClass().getSchool() != null) {
            schoolScopeService.assertSchoolAccess(score.getSchoolClass().getSchool().getUuid());
        }
        return ScoreResponse.from(score);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> list(UUID classUuid, Integer generation) {
        List<StudentScore> scores;
        if (classUuid != null && generation != null) {
            scores = scoreRepository.findDetailedByClassUuidAndGeneration(classUuid, generation);
        } else if (classUuid != null) {
            scores = scoreRepository.findDetailedByClassUuid(classUuid);
        } else if (generation != null) {
            scores = scoreRepository.findDetailedByGeneration(generation);
        } else {
            User current = schoolScopeService.requireCurrentUser();
            RoleName role = current.getRole() != null ? current.getRole().getName() : null;
            if (role == RoleName.TEACHER) {
                scores = scoreRepository.findDetailedByTeacherUuid(current.getUuid());
            } else if (role == RoleName.STUDENT) {
                scores = scoreRepository.findDetailedByStudentUuid(current.getUuid());
            } else {
                scores = scoreRepository.findAllDetailed();
            }
        }
        return scores.stream()
                .filter(this::scoreInScope)
                .map(ScoreResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listByStudent(UUID studentUuid, Integer generation, String term) {
        return loadStudentScores(studentUuid, generation, term).stream()
                .map(ScoreResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term) {
        List<StudentScore> scores = loadStudentScores(studentUuid, generation, term);
        User student = userRepository.findDetailedById(studentUuid)
                .orElseThrow(() -> new ExceptionNotFound("Student not found: " + studentUuid));
        return buildGpa(student, scores, generation, term);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGpaResponse getMyGpa(Integer generation, String term) {
        User current = schoolScopeService.requireCurrentUser();
        return getStudentGpa(current.getUuid(), generation, term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listMyScores(Integer generation, String term) {
        User current = schoolScopeService.requireCurrentUser();
        return listByStudent(current.getUuid(), generation, term);
    }

    @Override
    public void delete(UUID id) {
        StudentScore score = findScore(id);
        assertCanManage(score.getSchoolClass());
        scoreRepository.delete(score);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel(UUID classUuid, Integer generation) {
        ScoreExcelService excel = requireExcel();
        return excel.export(list(classUuid, generation), this::findEmail);
    }

    @Override
    public int importExcel(MultipartFile file, UUID classUuid) {
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(classUuid)
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + classUuid));
        assertCanManage(schoolClass);
        return requireExcel().importRows(file, classUuid, (request, email) -> {
            User student = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ExceptionNotFound("Student not found: " + email));
            request.setStudentUuid(student.getUuid());
            create(request);
        });
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
    public GpaSummaryStats gpaSummary() {
        List<StudentAgg> aggs = aggregateStudentGpas(scopedScores());
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
    public List<TopStudentRow> topStudentsByClass() {
        return topByGroup(aggregateStudentGpas(scopedScores()), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStudentRow> topStudentsByGrade() {
        return topByGroup(aggregateStudentGpas(scopedScores()), false);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries termScoreChart() {
        List<StudentScore> scores = scopedScores();
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

    private List<StudentScore> scopedScores() {
        List<StudentScore> all = scoreRepository.findAllDetailed();
        return all.stream().filter(this::scoreInScope).toList();
    }

    private boolean scoreInScope(StudentScore score) {
        if (score.getSchoolClass() != null && score.getSchoolClass().getSchool() != null) {
            boolean schoolOk = schoolScopeService.scopedSchoolUuid().isEmpty()
                    || schoolScopeService.scopedSchoolUuid().get()
                            .equals(score.getSchoolClass().getSchool().getUuid());
            if (!schoolOk) {
                return false;
            }
        }
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role != RoleName.TEACHER) {
            return true;
        }
        if (score.getSchoolClass() == null || current.getSchoolClasses() == null) {
            return false;
        }
        UUID classUuid = score.getSchoolClass().getUuid();
        return current.getSchoolClasses().stream().anyMatch(c -> c.getUuid().equals(classUuid));
    }

    private List<StudentAgg> aggregateStudentGpas(List<StudentScore> scores) {
        Map<UUID, List<StudentScore>> byStudent = new LinkedHashMap<>();
        for (StudentScore score : scores) {
            if (score.getStudent() == null) {
                continue;
            }
            byStudent.computeIfAbsent(score.getStudent().getUuid(), k -> new ArrayList<>()).add(score);
        }
        List<StudentAgg> result = new ArrayList<>();
        for (Map.Entry<UUID, List<StudentScore>> entry : byStudent.entrySet()) {
            StudentGpaResponse gpa = buildGpa(entry.getValue().get(0).getStudent(), entry.getValue(), null, null);
            User student = entry.getValue().get(0).getStudent();
            Map<UUID, String> classNames = new LinkedHashMap<>();
            Map<String, String> grades = new LinkedHashMap<>();
            for (StudentScore s : entry.getValue()) {
                if (s.getSchoolClass() != null) {
                    classNames.put(s.getSchoolClass().getUuid(), s.getSchoolClass().getName());
                    String grade = student.getGrade() != null && !student.getGrade().isBlank()
                            ? student.getGrade().trim()
                            : (s.getSchoolClass().getGrade() != null ? s.getSchoolClass().getGrade().trim() : "—");
                    grades.put(grade, grade);
                }
            }
            if (classNames.isEmpty() && student.getSchoolClasses() != null) {
                student.getSchoolClasses().forEach(c -> classNames.put(c.getUuid(), c.getName()));
            }
            if (grades.isEmpty()) {
                String g = student.getGrade() != null && !student.getGrade().isBlank() ? student.getGrade() : "—";
                grades.put(g, g);
            }
            result.add(new StudentAgg(
                    student.getUuid(),
                    student.getName(),
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

    private ScoreExcelService requireExcel() {
        ScoreExcelService excel = excelService.getIfAvailable();
        if (excel == null) {
            throw new IllegalStateException(
                    "Excel support unavailable. Restart the app fully after Maven refresh (Apache POI).");
        }
        return excel;
    }

    private StudentScore map(StudentScore score, ScoreRequest request) {
        User teacher = schoolScopeService.requireCurrentUser();
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
        assertCanManage(schoolClass);

        User student = userRepository.findDetailedById(request.getStudentUuid())
                .orElseThrow(() -> new ExceptionNotFound("Student not found: " + request.getStudentUuid()));
        if (student.getRole() == null || student.getRole().getName() != RoleName.STUDENT) {
            throw new IllegalArgumentException("Scores can only be assigned to students");
        }
        boolean inClass = student.getSchoolClasses() != null
                && student.getSchoolClasses().stream().anyMatch(c -> c.getUuid().equals(schoolClass.getUuid()));
        if (!inClass) {
            throw new IllegalArgumentException("Student does not belong to this class");
        }

        score.setStudent(student);
        score.setSchoolClass(schoolClass);
        score.setTeacher(teacher);
        score.setSubject(request.getSubject().trim());
        score.setTerm(blankTo(request.getTerm(), "Term 1"));
        score.setScore(request.getScore());
        score.setMaxScore(request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore());
        score.setRemark(blankToNull(request.getRemark()));
        return score;
    }

    private void assertCanManage(SchoolClass schoolClass) {
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role == RoleName.TEACHER) {
            boolean teaches = current.getSchoolClasses() != null
                    && current.getSchoolClasses().stream()
                            .anyMatch(c -> c.getUuid().equals(schoolClass.getUuid()));
            if (!teaches) {
                throw new AccessDeniedException("You can only score students in your classes");
            }
        }
    }

    private List<StudentScore> loadStudentScores(UUID studentUuid, Integer generation, String term) {
        User student = userRepository.findDetailedById(studentUuid)
                .orElseThrow(() -> new ExceptionNotFound("Student not found: " + studentUuid));
        assertCanViewStudentScores(student);

        List<StudentScore> scores = generation == null
                ? scoreRepository.findDetailedByStudentUuid(studentUuid)
                : scoreRepository.findDetailedByStudentUuidAndGeneration(studentUuid, generation);

        if (term != null && !term.isBlank()) {
            String wanted = term.trim().toLowerCase(Locale.ROOT);
            scores = scores.stream()
                    .filter(s -> s.getTerm() != null && s.getTerm().trim().toLowerCase(Locale.ROOT).equals(wanted))
                    .toList();
        }
        return scores;
    }

    private void assertCanViewStudentScores(User student) {
        User current = schoolScopeService.requireCurrentUser();
        if (current.getUuid().equals(student.getUuid())) {
            return;
        }
        if (!hasAuthority("SCORE_READ")) {
            throw new AccessDeniedException("You can only view your own scores");
        }
        if (student.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(student.getSchool().getUuid());
        }
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role == RoleName.TEACHER) {
            boolean sharesClass = current.getSchoolClasses() != null
                    && student.getSchoolClasses() != null
                    && current.getSchoolClasses().stream()
                            .anyMatch(tc -> student.getSchoolClasses().stream()
                                    .anyMatch(sc -> sc.getUuid().equals(tc.getUuid())));
            if (!sharesClass) {
                throw new AccessDeniedException("You can only view scores for students in your classes");
            }
        }
    }

    private static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private StudentGpaResponse buildGpa(User student, List<StudentScore> scores, Integer generation, String term) {
        List<ScoreResponse> scoreResponses = scores.stream().map(ScoreResponse::from).toList();
        Map<String, List<StudentScore>> bySubject = new LinkedHashMap<>();
        for (StudentScore score : scores) {
            String key = score.getSubject() == null ? "Unknown" : score.getSubject().trim();
            bySubject.computeIfAbsent(key, k -> new ArrayList<>()).add(score);
        }

        List<SubjectGradeSummary> subjects = new ArrayList<>();
        BigDecimal gpaSum = BigDecimal.ZERO;
        BigDecimal percentSum = BigDecimal.ZERO;
        int counted = 0;

        for (Map.Entry<String, List<StudentScore>> entry : bySubject.entrySet()) {
            BigDecimal subjectPercentSum = BigDecimal.ZERO;
            for (StudentScore score : entry.getValue()) {
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
                .studentUuid(student.getUuid())
                .studentName(student.getName())
                .studentEmail(student.getEmail())
                .generation(generation)
                .term(term)
                .gpa(gpa)
                .averagePercent(averagePercent)
                .letterGrade(GradeScale.letterGrade(averagePercent))
                .totalScores(scores.size())
                .subjects(subjects)
                .scores(scoreResponses)
                .build();
    }

    private StudentScore findScore(UUID id) {
        return scoreRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Score not found: " + id));
    }

    private String findEmail(UUID studentUuid) {
        if (studentUuid == null) {
            return "";
        }
        return userRepository.findById(studentUuid).map(User::getEmail).orElse("");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
