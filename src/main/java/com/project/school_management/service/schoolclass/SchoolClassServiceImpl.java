package com.project.school_management.service.schoolclass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolScopeService schoolScopeService;

    public SchoolClassServiceImpl(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            SchoolScopeService schoolScopeService) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
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
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return SchoolClassResponse.from(findClass(saved.getUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassResponse getById(UUID id) {
        SchoolClass schoolClass = findClass(id);
        if (schoolClass.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
        }
        assertTeacherClassAccess(schoolClass.getUuid());
        return SchoolClassResponse.from(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        List<SchoolClassResponse> classes = schoolScopeService.scopedSchoolUuid()
                .map(uuid -> schoolClassRepository.findDetailedBySchoolUuid(uuid).stream()
                        .map(SchoolClassResponse::from)
                        .toList())
                .orElseGet(() -> schoolClassRepository.findAllDetailed().stream()
                        .map(SchoolClassResponse::from)
                        .toList());
        return filterTeacherClasses(classes).stream()
                .sorted(java.util.Comparator.comparing(
                        c -> c.getName() == null ? "" : c.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getBySchool(UUID schoolUuid) {
        schoolScopeService.assertSchoolAccess(schoolUuid);
        return filterTeacherClasses(schoolClassRepository.findDetailedBySchoolUuid(schoolUuid).stream()
                .map(SchoolClassResponse::from)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getByGeneration(Integer generation) {
        return filterTeacherClasses(schoolClassRepository.findDetailedByGeneration(generation).stream()
                .filter(c -> c.getSchool() == null
                        || schoolScopeService.scopedSchoolUuid().isEmpty()
                        || schoolScopeService.scopedSchoolUuid().get().equals(c.getSchool().getUuid()))
                .map(SchoolClassResponse::from)
                .toList());
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
                .filter(g -> g != null)
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
        return SchoolClassResponse.from(findClass(id));
    }

    @Override
    public void delete(UUID id) {
        if (!schoolClassRepository.existsById(id)) {
            throw new ExceptionNotFound("Class not found: " + id);
        }
        schoolClassRepository.deleteById(id);
    }

    private SchoolClass findClass(UUID id) {
        return schoolClassRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + id));
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

    /** {@code null} = not a teacher (no extra filter). Empty set = teacher with no classes. */
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
