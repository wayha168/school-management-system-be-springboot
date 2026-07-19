package com.project.school_management.service.schoolclass;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.SchoolMag;
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
        return SchoolClassResponse.from(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        return schoolScopeService.scopedSchoolUuid()
                .map(this::getBySchool)
                .orElseGet(() -> schoolClassRepository.findAllDetailed().stream()
                        .map(SchoolClassResponse::from)
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getBySchool(UUID schoolUuid) {
        schoolScopeService.assertSchoolAccess(schoolUuid);
        return schoolClassRepository.findDetailedBySchoolUuid(schoolUuid).stream()
                .map(SchoolClassResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getByGeneration(Integer generation) {
        return schoolClassRepository.findDetailedByGeneration(generation).stream()
                .filter(c -> c.getSchool() == null
                        || schoolScopeService.scopedSchoolUuid().isEmpty()
                        || schoolScopeService.scopedSchoolUuid().get().equals(c.getSchool().getUuid()))
                .map(SchoolClassResponse::from)
                .toList();
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
}
