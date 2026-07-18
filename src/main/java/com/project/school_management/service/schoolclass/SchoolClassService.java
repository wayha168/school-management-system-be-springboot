package com.project.school_management.service.schoolclass;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;

public interface SchoolClassService {

    SchoolClassResponse create(SchoolClassRequest request);

    SchoolClassResponse getById(UUID id);

    List<SchoolClassResponse> getAll();

    List<SchoolClassResponse> getBySchool(UUID schoolUuid);

    SchoolClassResponse update(UUID id, SchoolClassRequest request);

    void delete(UUID id);
}
