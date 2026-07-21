package com.project.assessment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.assessment.entity.GpaAccessGrant;

public interface GpaAccessGrantRepository extends JpaRepository<GpaAccessGrant, UUID> {
}
