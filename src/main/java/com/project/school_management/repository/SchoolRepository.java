package com.project.school_management.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.school_management.entities.SchoolMag;

public interface SchoolRepository extends JpaRepository<SchoolMag, UUID> {
}
