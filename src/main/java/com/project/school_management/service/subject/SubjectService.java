package com.project.school_management.service.subject;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.subject.SubjectRequest;
import com.project.school_management.dto.subject.SubjectResponse;

public interface SubjectService {

    SubjectResponse create(SubjectRequest request);

    SubjectResponse getById(UUID id);

    List<SubjectResponse> getAll();

    List<SubjectResponse> getBySchool(UUID schoolUuid);

    SubjectResponse update(UUID id, SubjectRequest request);

    void delete(UUID id);
}
