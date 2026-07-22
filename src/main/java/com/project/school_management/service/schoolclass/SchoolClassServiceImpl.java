package com.project.school_management.service.schoolclass;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class SchoolClassServiceImpl implements SchoolClassService {

    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 6;
    private static final SecureRandom JOIN_CODE_RANDOM = new SecureRandom();

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolScopeService schoolScopeService;

    public SchoolClassServiceImpl(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolScopeService schoolScopeService) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    public SchoolClassResponse create(SchoolClassRequest request) {
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.getName());
        schoolClass.setGrade(request.getGrade());
        schoolClass.setGeneration(request.getGeneration());
        schoolClass.setAcademicYear(request.getAcademicYear());
        schoolClass.setSchool(findSchool(request.getSchoolUuid()));
        schoolClass.setSubjects(normalizeSubjects(request.getSubjects()));
        schoolClass.setJoinCode(generateUniqueJoinCode());
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        if (request.getTeacherUuids() != null) {
            syncTeachers(saved, request.getTeacherUuids());
        }
        return toResponse(findClass(saved.getUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassResponse getById(UUID id) {
        SchoolClass schoolClass = findClass(id);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        assertTeacherClassAccess(schoolClass.getUuid());
        return toResponse(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        List<SchoolClass> entities = schoolScopeService.scopedSchoolUuid()
                .map(schoolClassRepository::findDetailedBySchoolUuid)
                .orElseGet(schoolClassRepository::findAllDetailed);
        List<SchoolClassResponse> classes = toResponses(entities);
        return filterTeacherClasses(classes).stream()
                .sorted(Comparator.comparing(
                        c -> c.getName() == null ? "" : c.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getBySchool(UUID schoolUuid) {
        schoolScopeService.assertSchoolAccess(schoolUuid);
        return filterTeacherClasses(toResponses(schoolClassRepository.findDetailedBySchoolUuid(schoolUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getByGeneration(Integer generation) {
        return filterTeacherClasses(toResponses(schoolClassRepository.findDetailedByGeneration(generation).stream()
                .filter(c -> c.getSchool() == null
                        || schoolScopeService.scopedSchoolUuid().isEmpty()
                        || schoolScopeService.scopedSchoolUuid().get().equals(c.getSchool().getUuid()))
                .toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getByGrade(String grade) {
        if (grade == null || grade.isBlank()) {
            return getAll();
        }
        String wanted = grade.trim().toLowerCase();
        return getAll().stream()
                .filter(c -> c.getGrade() != null && c.getGrade().trim().toLowerCase().equals(wanted))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> filter(Integer generation, String grade) {
        return getAll().stream()
                .filter(c -> generation == null || generation.equals(c.getGeneration()))
                .filter(c -> grade == null || grade.isBlank()
                        || (c.getGrade() != null && c.getGrade().trim().equalsIgnoreCase(grade.trim())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> listGenerations() {
        return getAll().stream()
                .map(SchoolClassResponse::getGeneration)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listGrades() {
        return getAll().stream()
                .map(SchoolClassResponse::getGrade)
                .filter(g -> g != null && !g.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public SchoolClassResponse update(UUID id, SchoolClassRequest request) {
        SchoolClass schoolClass = findClass(id);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());
        schoolClass.setName(request.getName());
        schoolClass.setGrade(request.getGrade());
        schoolClass.setGeneration(request.getGeneration());
        schoolClass.setAcademicYear(request.getAcademicYear());
        schoolClass.setSchool(findSchool(request.getSchoolUuid()));
        schoolClass.setSubjects(normalizeSubjects(request.getSubjects()));
        schoolClassRepository.save(schoolClass);
        if (request.getTeacherUuids() != null) {
            syncTeachers(schoolClass, request.getTeacherUuids());
        }
        return toResponse(findClass(id));
    }

    @Override
    public void delete(UUID id) {
        if (!schoolClassRepository.existsById(id)) {
            throw new ExceptionNotFound("Class not found: " + id);
        }
        schoolClassRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listAssignableTeachers() {
        List<User> teachers = schoolScopeService.scopedSchoolUuid()
                .map(uuid -> userRepository.findDetailedBySchoolUuidAndRole(uuid, RoleName.TEACHER))
                .orElseGet(() -> userRepository.findDetailedByRole(RoleName.TEACHER));
        return teachers.stream()
                .map(UserResponse::from)
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> teachersForClass(UUID classUuid) {
        SchoolClass schoolClass = findClass(classUuid);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        assertTeacherClassAccess(classUuid);
        return userRepository.findDetailedByClassAndRole(classUuid, RoleName.TEACHER).stream()
                .map(UserResponse::from)
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> membersForClass(UUID classUuid) {
        SchoolClass schoolClass = findClass(classUuid);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        assertTeacherClassAccess(classUuid);
        return userRepository.findDetailedByClassUuid(classUuid).stream()
                .map(UserResponse::from)
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public SchoolClassResponse joinByCode(String joinCode) {
        String code = normalizeJoinCode(joinCode);
        SchoolClass schoolClass = schoolClassRepository.findDetailedByJoinCode(code)
                .orElseThrow(() -> new ExceptionNotFound("Invalid class join code"));
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }

        User current = schoolScopeService.requireCurrentUser();
        User user = userRepository.findDetailedById(current.getUuid())
                .orElseThrow(() -> new ExceptionNotFound("User not found"));

        if (user.getSchoolClasses() == null) {
            user.setSchoolClasses(new ArrayList<>());
        }
        boolean already = user.getSchoolClasses().stream()
                .anyMatch(c -> schoolClass.getUuid().equals(c.getUuid()));
        if (!already) {
            user.getSchoolClasses().add(schoolClass);
            userRepository.save(user);
        }
        return toResponse(findClass(schoolClass.getUuid()));
    }

    @Override
    public SchoolClassResponse regenerateJoinCode(UUID classUuid) {
        SchoolClass schoolClass = findClass(classUuid);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        schoolClass.setJoinCode(generateUniqueJoinCode());
        schoolClassRepository.save(schoolClass);
        return toResponse(findClass(classUuid));
    }

    /**
     * Assign / unassign teachers for a class via the owning
     * {@code User.schoolClasses} side.
     * Student enrollments on the same join table are left untouched.
     */
    private void syncTeachers(SchoolClass schoolClass, List<UUID> teacherUuids) {
        UUID classId = schoolClass.getUuid();
        Set<UUID> desired = teacherUuids == null
                ? Set.of()
                : teacherUuids.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<User> currentTeachers = userRepository.findDetailedByClassAndRole(classId, RoleName.TEACHER);
        Set<UUID> currentIds = currentTeachers.stream()
                .map(User::getUuid)
                .collect(Collectors.toSet());

        for (User teacher : currentTeachers) {
            if (!desired.contains(teacher.getUuid())) {
                teacher.getSchoolClasses().removeIf(c -> classId.equals(c.getUuid()));
                userRepository.save(teacher);
            }
        }

        for (UUID teacherId : desired) {
            if (currentIds.contains(teacherId)) {
                continue;
            }
            User teacher = userRepository.findDetailedById(teacherId)
                    .orElseThrow(() -> new ExceptionNotFound("Teacher not found: " + teacherId));
            if (teacher.getRole() == null || teacher.getRole().getName() != RoleName.TEACHER) {
                throw new IllegalArgumentException("Only teachers can be assigned to a class");
            }
            if (teacher.getSchool() != null) {
                schoolScopeService.assertSchoolAccess(teacher.getSchool().getUuid());
            }
            if (schoolClass.getSchool() != null && teacher.getSchool() != null
                    && !schoolClass.getSchool().getUuid().equals(teacher.getSchool().getUuid())) {
                throw new IllegalArgumentException(
                        "Teacher " + teacher.getName() + " belongs to a different school");
            }
            if (teacher.getSchoolClasses() == null) {
                teacher.setSchoolClasses(new ArrayList<>());
            }
            boolean already = teacher.getSchoolClasses().stream()
                    .anyMatch(c -> classId.equals(c.getUuid()));
            if (!already) {
                teacher.getSchoolClasses().add(schoolClass);
                userRepository.save(teacher);
            }
        }
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        List<User> teachers = userRepository.findDetailedByClassAndRole(
                schoolClass.getUuid(), RoleName.TEACHER);
        return toResponse(schoolClass, teachers);
    }

    /**
     * Batch-map teachers onto classes so admin/superadmin lists stay fast and show
     * Belongs to.
     */
    private List<SchoolClassResponse> toResponses(List<SchoolClass> classes) {
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<User>> teachersByClass = new HashMap<>();
        for (User teacher : userRepository.findDetailedByRole(RoleName.TEACHER)) {
            if (teacher.getSchoolClasses() == null) {
                continue;
            }
            for (SchoolClass taught : teacher.getSchoolClasses()) {
                if (taught == null || taught.getUuid() == null) {
                    continue;
                }
                teachersByClass
                        .computeIfAbsent(taught.getUuid(), key -> new ArrayList<>())
                        .add(teacher);
            }
        }
        return classes.stream()
                .map(c -> toResponse(c, teachersByClass.getOrDefault(c.getUuid(), List.of())))
                .toList();
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass, List<User> teachers) {
        return toResponse(schoolClass, teachers, -1);
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass, List<User> teachers, int studentCount) {
        List<User> safe = teachers == null ? List.of() : teachers;
        List<UUID> teacherUuids = safe.stream().map(User::getUuid).toList();
        List<String> teacherNames = safe.stream()
                .map(User::getName)
                .filter(n -> n != null && !n.isBlank())
                .toList();
        int students = studentCount >= 0
                ? studentCount
                : (int) userRepository.countByClassUuidAndRole(schoolClass.getUuid(), RoleName.STUDENT);
        return SchoolClassResponse.from(schoolClass, teacherUuids, teacherNames, teacherNames.size(), students);
    }

    private SchoolClass findClass(UUID id) {
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + id));
        ensureJoinCode(schoolClass);
        return schoolClass;
    }

    private void ensureJoinCode(SchoolClass schoolClass) {
        if (schoolClass.getJoinCode() != null && !schoolClass.getJoinCode().isBlank()) {
            return;
        }
        schoolClass.setJoinCode(generateUniqueJoinCode());
        schoolClassRepository.save(schoolClass);
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < 40; attempt++) {
            String code = randomJoinCode();
            if (!schoolClassRepository.existsByJoinCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique class join code");
    }

    private static String randomJoinCode() {
        StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            sb.append(JOIN_CODE_ALPHABET.charAt(JOIN_CODE_RANDOM.nextInt(JOIN_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String normalizeJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Join code is required");
        }
        return joinCode.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }

    private SchoolMag findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("School not found: " + id));
    }

    private static List<String> normalizeSubjects(List<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>();
        for (String raw : subjects) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String subject = raw.trim();
            String key = subject.toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                cleaned.add(subject);
            }
        }
        return cleaned;
    }

    /** Teachers only see / open classes they are assigned to. */
    private List<SchoolClassResponse> filterTeacherClasses(List<SchoolClassResponse> classes) {
        Set<UUID> taught = taughtClassUuidsOrNull();
        if (taught == null) {
            return classes;
        }
        return classes.stream().filter(c -> taught.contains(c.getUuid())).toList();
    }

    private void assertTeacherClassAccess(UUID classUuid) {
        Set<UUID> taught = taughtClassUuidsOrNull();
        if (taught != null && !taught.contains(classUuid)) {
            throw new AccessDeniedException("You can only access classes assigned to you");
        }
    }

    /**
     * {@code null} = not a teacher (no extra filter). Empty set = teacher with no
     * classes.
     */
    private Set<UUID> taughtClassUuidsOrNull() {
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role != RoleName.TEACHER) {
            return null;
        }
        if (current.getSchoolClasses() == null || current.getSchoolClasses().isEmpty()) {
            return Set.of();
        }
        return current.getSchoolClasses().stream()
                .map(SchoolClass::getUuid)
                .collect(Collectors.toSet());
    }
}
