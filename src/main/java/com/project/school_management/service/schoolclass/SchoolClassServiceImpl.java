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

@Service
@Transactional
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;

    public SchoolClassServiceImpl(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    public SchoolClassResponse create(SchoolClassRequest request) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.getName());
        schoolClass.setGrade(request.getGrade());
        schoolClass.setSchool(findSchool(request.getSchoolUuid()));
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return SchoolClassResponse.from(findClass(saved.getUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassResponse getById(UUID id) {
        return SchoolClassResponse.from(findClass(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        return schoolClassRepository.findAllDetailed().stream().map(SchoolClassResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getBySchool(UUID schoolUuid) {
        return schoolClassRepository.findDetailedBySchoolUuid(schoolUuid).stream()
                .map(SchoolClassResponse::from)
                .toList();
    }

    @Override
    public SchoolClassResponse update(UUID id, SchoolClassRequest request) {
        SchoolClass schoolClass = findClass(id);
        schoolClass.setName(request.getName());
        schoolClass.setGrade(request.getGrade());
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
