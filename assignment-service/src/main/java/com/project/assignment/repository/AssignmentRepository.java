package com.project.assignment.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.assignment.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByClassUuidOrderByCreatedAtDesc(UUID classUuid);

    List<Assignment> findByClassUuidInAndStatusOrderByDueAtAsc(Collection<UUID> classUuids, String status);
}
