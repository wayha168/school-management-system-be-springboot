package com.project.school_management.service.score;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.attendance.ClassAttendanceOverview;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.dto.dashboard.TopStudentRow;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.score.ScoreBatchRequest;
import com.project.school_management.dto.score.ScoreMarkItem;
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserClassItem;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.user.UserService;

/**
 * BFF score service: validates school/class membership locally, persists via assessment microservice.
 */
@Service
@Transactional
public class ScoreServiceImpl implements ScoreService {

    private final AssessmentClient assessmentClient;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolScopeService schoolScopeService;
    private final SchoolClassService schoolClassService;
    private final UserService userService;
    private final ObjectProvider<ScoreExcelService> excelService;

    public ScoreServiceImpl(
            AssessmentClient assessmentClient,
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolScopeService schoolScopeService,
            SchoolClassService schoolClassService,
            UserService userService,
            ObjectProvider<ScoreExcelService> excelService) {
        this.assessmentClient = assessmentClient;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolScopeService = schoolScopeService;
        this.schoolClassService = schoolClassService;
        this.userService = userService;
        this.excelService = excelService;
    }

    @Override
    public ScoreResponse create(ScoreRequest request) {
        validateScoreBounds(request.getScore(), request.getMaxScore());
        Map<String, Object> body = buildUpsertBody(request);
        return assessmentClient.create(body).toDto();
    }

    @Override
    public ScoreResponse update(UUID id, ScoreRequest request) {
        validateScoreBounds(request.getScore(), request.getMaxScore());
        Map<String, Object> body = buildUpsertBody(request);
        return assessmentClient.update(id, body).toDto();
    }

    @Override
    public int upsertBatch(ScoreBatchRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No student scores to save");
        }
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
        assertCanManage(schoolClass);
        User teacher = schoolScopeService.requireCurrentUser();

        BigDecimal maxScore = request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore();
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max score must be greater than 0");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ScoreMarkItem item : request.getItems()) {
            if (item == null || item.getStudentUuid() == null || item.getScore() == null) {
                continue;
            }
            validateScoreBounds(item.getScore(), maxScore);
            User student = requireStudentInClass(item.getStudentUuid(), schoolClass);
            Map<String, Object> row = new HashMap<>();
            row.put("studentUuid", student.getUuid());
            row.put("studentName", student.getName());
            row.put("studentEmail", student.getEmail());
            row.put("studentGrade", student.getGrade());
            row.put("subject", item.getSubject());
            row.put("score", item.getScore());
            row.put("remark", item.getRemark());
            items.add(row);
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one student score");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("classUuid", schoolClass.getUuid());
        body.put("className", schoolClass.getName());
        body.put("generation", schoolClass.getGeneration());
        body.put("academicYear", schoolClass.getAcademicYear());
        body.put("schoolUuid", schoolClass.getSchool() != null ? schoolClass.getSchool().getUuid() : null);
        body.put("teacherUuid", teacher.getUuid());
        body.put("teacherName", teacher.getName());
        body.put("subject", request.getSubject());
        body.put("term", request.getTerm());
        body.put("maxScore", maxScore);
        body.put("items", items);
        return assessmentClient.upsertBatch(body);
    }

    @Override
    public int upsertClassSession(
            UUID classUuid,
            String period,
            String subject,
            BigDecimal maxScore,
            List<UUID> studentUuids,
            List<String> entryScores) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Select a subject before saving");
        }
        if (studentUuids == null || entryScores == null) {
            throw new IllegalArgumentException("No score rows submitted");
        }
        String term = resolveTermFromPeriod(period);
        ScoreBatchRequest batch = new ScoreBatchRequest();
        batch.setClassUuid(classUuid);
        batch.setTerm(term);
        batch.setSubject(subject.trim());
        batch.setMaxScore(maxScore);
        List<ScoreMarkItem> items = new ArrayList<>();
        int n = Math.min(studentUuids.size(), entryScores.size());
        for (int i = 0; i < n; i++) {
            String raw = entryScores.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            ScoreMarkItem item = new ScoreMarkItem();
            item.setStudentUuid(studentUuids.get(i));
            item.setSubject(subject.trim());
            item.setScore(new BigDecimal(raw.trim()));
            items.add(item);
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one student score before saving");
        }
        batch.setItems(items);
        return upsertBatch(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listForSession(UUID classUuid, String subject, String term) {
        if (classUuid == null) {
            return List.of();
        }
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(classUuid)
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + classUuid));
        assertCanManage(schoolClass);
        return AssessmentClient.toDtos(assessmentClient.listForSession(classUuid, subject, term));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ScoreResponse> scoresByStudentForSession(UUID classUuid, String subject, String term) {
        if (classUuid == null || subject == null || subject.isBlank()) {
            return Map.of();
        }
        return listForSession(classUuid, subject, term).stream()
                .filter(s -> s.getStudentUuid() != null)
                .collect(Collectors.toMap(
                        ScoreResponse::getStudentUuid,
                        s -> s,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResponse getById(UUID id) {
        return assessmentClient.getById(id).toDto();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> list(UUID classUuid, Integer generation) {
        return AssessmentClient.toDtos(assessmentClient.list(classUuid, generation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreResponse> listByStudent(UUID studentUuid, Integer generation, String term) {
        assertCanViewStudent(studentUuid);
        return AssessmentClient.toDtos(assessmentClient.listByStudent(studentUuid, generation, term));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term) {
        assertCanViewStudent(studentUuid);
        StudentGpaResponse gpa = assessmentClient.getStudentGpa(studentUuid, generation, term);
        if (gpa != null && (gpa.getStudentName() == null || gpa.getStudentEmail() == null)) {
            User student = userRepository.findDetailedById(studentUuid).orElse(null);
            if (student != null) {
                return StudentGpaResponse.builder()
                        .studentUuid(gpa.getStudentUuid() != null ? gpa.getStudentUuid() : studentUuid)
                        .studentName(gpa.getStudentName() != null ? gpa.getStudentName() : student.getName())
                        .studentEmail(gpa.getStudentEmail() != null ? gpa.getStudentEmail() : student.getEmail())
                        .generation(gpa.getGeneration())
                        .term(gpa.getTerm())
                        .gpa(gpa.getGpa())
                        .averagePercent(gpa.getAveragePercent())
                        .letterGrade(gpa.getLetterGrade())
                        .totalScores(gpa.getTotalScores())
                        .subjects(gpa.getSubjects())
                        .scores(gpa.getScores())
                        .build();
            }
        }
        return gpa;
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
        assessmentClient.delete(id);
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
            // Upsert via batch of one to avoid unique-constraint create failures
            ScoreBatchRequest batch = new ScoreBatchRequest();
            batch.setClassUuid(classUuid);
            batch.setSubject(request.getSubject());
            batch.setTerm(request.getTerm());
            batch.setMaxScore(request.getMaxScore());
            ScoreMarkItem item = new ScoreMarkItem();
            item.setStudentUuid(student.getUuid());
            item.setSubject(request.getSubject());
            item.setScore(request.getScore());
            item.setRemark(request.getRemark());
            batch.setItems(List.of(item));
            upsertBatch(batch);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listScoreGenerations() {
        return assessmentClient.listGenerations(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listScoreTerms() {
        return assessmentClient.listTerms(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listScoreGenerationsForStudent(UUID studentUuid) {
        return assessmentClient.listGenerations(studentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listScoreTermsForStudent(UUID studentUuid) {
        return assessmentClient.listTerms(studentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listStudentUuidsWithScores(Integer generation) {
        if (generation == null) {
            return List.of();
        }
        return assessmentClient.listStudentUuids(generation);
    }

    @Override
    @Transactional(readOnly = true)
    public GpaSummaryStats gpaSummary() {
        try {
            GpaSummaryStats stats = assessmentClient.gpaSummary();
            return stats != null ? stats : emptyGpaSummary();
        } catch (Exception ex) {
            return emptyGpaSummary();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStudentRow> topStudentsByClass() {
        try {
            return assessmentClient.topByClass();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopStudentRow> topStudentsByGrade() {
        try {
            return assessmentClient.topByGrade();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries termScoreChart() {
        try {
            ChartSeries chart = assessmentClient.termChart();
            return chart != null ? chart : ChartSeries.builder().labels(List.of()).values(List.of()).build();
        } catch (Exception ex) {
            return ChartSeries.builder().labels(List.of()).values(List.of()).build();
        }
    }

    private static GpaSummaryStats emptyGpaSummary() {
        return GpaSummaryStats.builder()
                .averageGpa(BigDecimal.ZERO)
                .averagePercent(BigDecimal.ZERO)
                .studentsWithScores(0)
                .totalScoreRows(0)
                .topLetter("—")
                .build();
    }

    private Map<String, Object> buildUpsertBody(ScoreRequest request) {
        User teacher = schoolScopeService.requireCurrentUser();
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
        assertCanManage(schoolClass);
        User student = requireStudentInClass(request.getStudentUuid(), schoolClass);

        Map<String, Object> body = new HashMap<>();
        body.put("studentUuid", student.getUuid());
        body.put("studentName", student.getName());
        body.put("studentEmail", student.getEmail());
        body.put("studentGrade", student.getGrade());
        body.put("schoolUuid", schoolClass.getSchool() != null ? schoolClass.getSchool().getUuid() : null);
        body.put("classUuid", schoolClass.getUuid());
        body.put("className", schoolClass.getName());
        body.put("generation", schoolClass.getGeneration());
        body.put("academicYear", schoolClass.getAcademicYear());
        body.put("teacherUuid", teacher.getUuid());
        body.put("teacherName", teacher.getName());
        body.put("subject", request.getSubject());
        body.put("term", request.getTerm());
        body.put("score", request.getScore());
        body.put("maxScore", request.getMaxScore() == null ? BigDecimal.valueOf(100) : request.getMaxScore());
        body.put("remark", request.getRemark());
        return body;
    }

    private User requireStudentInClass(UUID studentUuid, SchoolClass schoolClass) {
        User student = userRepository.findDetailedById(studentUuid)
                .orElseThrow(() -> new ExceptionNotFound("Student not found: " + studentUuid));
        if (student.getRole() == null || student.getRole().getName() != RoleName.STUDENT) {
            throw new IllegalArgumentException("Scores can only be assigned to students");
        }
        boolean inClass = student.getSchoolClasses() != null
                && student.getSchoolClasses().stream().anyMatch(c -> c.getUuid().equals(schoolClass.getUuid()));
        if (!inClass) {
            throw new IllegalArgumentException("Student does not belong to this class");
        }
        return student;
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

    private void assertCanViewStudent(UUID studentUuid) {
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role == RoleName.STUDENT) {
            if (!current.getUuid().equals(studentUuid)) {
                throw new AccessDeniedException("You can only view your own scores");
            }
            return;
        }
        User student = userRepository.findDetailedById(studentUuid)
                .orElseThrow(() -> new ExceptionNotFound("Student not found: " + studentUuid));
        if (student.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(student.getSchool().getUuid());
        }
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

    private ScoreExcelService requireExcel() {
        ScoreExcelService excel = excelService.getIfAvailable();
        if (excel == null) {
            throw new IllegalStateException(
                    "Excel support unavailable. Restart the app fully after Maven refresh (Apache POI).");
        }
        return excel;
    }

    private String findEmail(UUID studentUuid) {
        if (studentUuid == null) {
            return "";
        }
        return userRepository.findById(studentUuid).map(User::getEmail).orElse("");
    }

    private static void validateScoreBounds(BigDecimal score, BigDecimal maxScore) {
        BigDecimal max = maxScore == null ? BigDecimal.valueOf(100) : maxScore;
        if (score == null) {
            throw new IllegalArgumentException("Score is required");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max score must be greater than 0");
        }
        if (score.compareTo(max) > 0) {
            throw new IllegalArgumentException("Score cannot exceed max score (" + max + ")");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> visibleClassesForScoring(DataUser account, Integer generation) {
        List<SchoolClassResponse> classes = generation == null
                ? schoolClassService.getAll()
                : schoolClassService.getByGeneration(generation);
        if (account != null && account.getRole() == RoleName.TEACHER) {
            Set<UUID> taught = account.getClasses() == null
                    ? Set.of()
                    : account.getClasses().stream()
                            .map(UserClassItem::getUuid)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
            classes = classes.stream().filter(c -> taught.contains(c.getUuid())).toList();
        }
        return classes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassAttendanceOverview> buildClassOverviews(List<SchoolClassResponse> classes) {
        List<UserResponse> allUsers = userService.getAll();
        List<ClassAttendanceOverview> rows = new ArrayList<>();
        for (SchoolClassResponse c : classes) {
            List<String> teacherNames = allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.TEACHER)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .map(UserResponse::getName)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            int studentCount = (int) allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.STUDENT)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .count();
            String teachersLabel = teacherNames.isEmpty() ? "Unassigned" : String.join(", ", teacherNames);
            rows.add(ClassAttendanceOverview.builder()
                    .classUuid(c.getUuid())
                    .className(c.getName())
                    .grade(c.getGrade())
                    .generation(c.getGeneration())
                    .generationCode(c.getGenerationCode())
                    .schoolName(c.getSchoolName())
                    .teacherNames(teacherNames)
                    .teachersLabel(teachersLabel)
                    .studentCount(studentCount)
                    .build());
        }
        rows.sort(Comparator.comparing(ClassAttendanceOverview::getClassName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> studentsInClass(UUID classUuid) {
        return userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.STUDENT)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(UserResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean canEnterScores(DataUser account) {
        if (account == null || account.getRole() == null) {
            return false;
        }
        return account.getRole() == RoleName.SUPERADMIN
                || account.getRole() == RoleName.ADMIN
                || account.getRole() == RoleName.TEACHER
                || account.getRole() == RoleName.PRINCIPAL;
    }

    @Override
    public String normalizePeriodKey(String period) {
        if (period == null || period.isBlank()) {
            return "term1";
        }
        String key = period.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (key) {
            case "monthly", "month" -> "monthly";
            case "term2", "term_2" -> "term2";
            case "midterm", "mid_term", "mid-term" -> "midterm";
            case "final", "finale" -> "final";
            case "term1", "term_1", "term" -> "term1";
            default -> periodKeyFromTerm(period);
        };
    }

    @Override
    public String resolveTermFromPeriod(String periodKey) {
        return switch (normalizePeriodKey(periodKey)) {
            case "monthly" -> "Monthly " + YearMonth.now();
            case "term2" -> "Term 2";
            case "midterm" -> "Midterm";
            case "final" -> "Final";
            default -> "Term 1";
        };
    }

    @Override
    public String resolveTermFromForm(String term) {
        if (term == null || term.isBlank()) {
            return "Term 1";
        }
        String t = term.trim();
        if (t.equalsIgnoreCase("Monthly") || t.toLowerCase(Locale.ROOT).startsWith("monthly")) {
            return "Monthly " + YearMonth.now();
        }
        return resolveTermFromPeriod(periodKeyFromTerm(t));
    }

    @Override
    public String currentMonthlyLabel() {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
    }

    @Override
    public String resolveSubject(List<String> classSubjects, String requestedSubject) {
        List<String> subjects = classSubjects == null ? List.of() : classSubjects;
        if (requestedSubject == null || requestedSubject.isBlank()) {
            return subjects.isEmpty() ? null : subjects.get(0);
        }
        final String wanted = requestedSubject.trim();
        return subjects.stream()
                .filter(s -> s.equalsIgnoreCase(wanted))
                .findFirst()
                .orElse(wanted);
    }

    private static String periodKeyFromTerm(String term) {
        if (term == null || term.isBlank()) {
            return "term1";
        }
        String t = term.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("monthly")) {
            return "monthly";
        }
        if (t.equals("term 2") || t.equals("term2")) {
            return "term2";
        }
        if (t.equals("midterm") || t.equals("mid term")) {
            return "midterm";
        }
        if (t.equals("final")) {
            return "final";
        }
        return "term1";
    }
}
