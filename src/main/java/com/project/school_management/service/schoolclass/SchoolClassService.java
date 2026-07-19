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

    List<SchoolClassResponse> getByGeneration(Integer generation);

    List<SchoolClassResponse> getByGrade(String grade);

    List<SchoolClassResponse> filter(Integer generation, String grade);

    List<Integer> listGenerations();

    List<String> listGrades();

    SchoolClassResponse update(UUID id, SchoolClassRequest request);

    void delete(UUID id);
}
