package com.project.assessment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.assessment.dto.GpaAccessRequest;
import com.project.assessment.dto.GpaAccessResponse;
import com.project.assessment.entity.GpaAccessGrant;
import com.project.assessment.repository.GpaAccessGrantRepository;

@Service
@Transactional
public class GpaAccessService {

    private final GpaAccessGrantRepository grantRepository;

    public GpaAccessService(GpaAccessGrantRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    public GpaAccessResponse setAccess(UUID studentUuid, GpaAccessRequest request) {
        GpaAccessGrant grant = grantRepository.findById(studentUuid).orElseGet(GpaAccessGrant::new);
        grant.setStudentUuid(studentUuid);
        grant.setApproved(request.isApproved());
        grant.setApprovedByUuid(request.getApprovedByUuid());
        grantRepository.save(grant);
        return GpaAccessResponse.builder()
                .studentUuid(studentUuid)
                .approved(grant.isApproved())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedAccess(UUID studentUuid) {
        return grantRepository.findById(studentUuid)
                .map(GpaAccessGrant::isApproved)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public GpaAccessResponse getAccess(UUID studentUuid) {
        boolean approved = hasApprovedAccess(studentUuid);
        return GpaAccessResponse.builder()
                .studentUuid(studentUuid)
                .approved(approved)
                .build();
    }
}
