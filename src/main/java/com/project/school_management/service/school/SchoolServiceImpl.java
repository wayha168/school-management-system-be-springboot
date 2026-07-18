package com.project.school_management.service.school;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.school.SchoolResponse;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolRepository;

@Service
@Transactional
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolServiceImpl(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Override
    public SchoolResponse create(SchoolRequest request) {
        SchoolMag school = map(new SchoolMag(), request);
        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse getById(UUID id) {
        return SchoolResponse.from(findSchool(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolResponse> getAll() {
        return schoolRepository.findAll().stream().map(SchoolResponse::from).toList();
    }

    @Override
    public SchoolResponse update(UUID id, SchoolRequest request) {
        SchoolMag school = map(findSchool(id), request);
        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Override
    public void delete(UUID id) {
        if (!schoolRepository.existsById(id)) {
            throw new ExceptionNotFound("School not found: " + id);
        }
        schoolRepository.deleteById(id);
    }

    private SchoolMag findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("School not found: " + id));
    }

    private SchoolMag map(SchoolMag school, SchoolRequest request) {
        school.setName(request.getName());
        school.setDescription(request.getDescription());
        school.setAddress(request.getAddress());
        school.setPhone(request.getPhone());
        school.setEmail(request.getEmail());
        school.setWebsite(request.getWebsite());
        school.setLogo(request.getLogo());
        school.setBanner(request.getBanner());
        return school;
    }
}
