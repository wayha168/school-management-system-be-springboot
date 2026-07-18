package com.project.school_management.service.school;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.school.SchoolResponse;

public interface SchoolService {

    SchoolResponse create(SchoolRequest request);

    SchoolResponse getById(UUID id);

    List<SchoolResponse> getAll();

    SchoolResponse update(UUID id, SchoolRequest request);

    void delete(UUID id);
}
