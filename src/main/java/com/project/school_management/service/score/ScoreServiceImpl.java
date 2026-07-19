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
                .filter(s -> s.getSchoolClass() == null
                        || s.getSchoolClass().getSchool() == null
                        || schoolScopeService.scopedSchoolUuid().isEmpty()
                        || schoolScopeService.scopedSchoolUuid().get()
                                .equals(s.getSchoolClass().getSchool().getUuid()))
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
