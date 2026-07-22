package com.project.school_management.service.schoolclass;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.UserResponse;

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

    /** Teachers that can be assigned to classes (school-scoped). */
    List<UserResponse> listAssignableTeachers();

    /** Teachers currently belonging to a class. */
    List<UserResponse> teachersForClass(UUID classUuid);

    /** All members (students + teachers) in a class. */
    List<UserResponse> membersForClass(UUID classUuid);

    /** Current user joins a class using its join code. */
    SchoolClassResponse joinByCode(String joinCode);

    /** Regenerate the class join code (admin/teacher with write). */
    SchoolClassResponse regenerateJoinCode(UUID classUuid);
}
