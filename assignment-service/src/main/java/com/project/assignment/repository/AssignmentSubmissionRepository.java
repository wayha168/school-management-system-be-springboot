package com.project.assignment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.assignment.entity.AssignmentSubmission;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    List<AssignmentSubmission> findByAssignmentUuidOrderBySubmittedAtDesc(UUID assignmentUuid);

    Optional<AssignmentSubmission> findByAssignmentUuidAndStudentUuid(UUID assignmentUuid, UUID studentUuid);
}
