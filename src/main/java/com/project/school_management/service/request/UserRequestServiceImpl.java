package com.project.school_management.service.request;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.request.GpaAccessStatus;
import com.project.school_management.dto.request.UserRequestDto;
import com.project.school_management.dto.request.UserRequestReplyDto;
import com.project.school_management.dto.request.UserRequestResponse;
import com.project.school_management.entities.User;
import com.project.school_management.entities.UserRequest;
import com.project.school_management.enums.RequestCategory;
import com.project.school_management.enums.RequestStatus;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.UserRequestRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.service.score.AssessmentClient;

@Service
@Transactional
public class UserRequestServiceImpl implements UserRequestService {

    private static final Set<RoleName> GPA_APPROVERS = EnumSet.of(
            RoleName.SUPERADMIN, RoleName.ADMIN, RoleName.PRINCIPAL);

    private static final Set<RequestStatus> PENDING_STATUSES = EnumSet.of(
            RequestStatus.OPEN, RequestStatus.IN_PROGRESS);

    private final UserRequestRepository requestRepository;
    private final SchoolScopeService schoolScopeService;
    private final AssessmentClient assessmentClient;

    public UserRequestServiceImpl(
            UserRequestRepository requestRepository,
            SchoolScopeService schoolScopeService,
            AssessmentClient assessmentClient) {
        this.requestRepository = requestRepository;
        this.schoolScopeService = schoolScopeService;
        this.assessmentClient = assessmentClient;
    }

    @Override
    public UserRequestResponse create(UserRequestDto dto) {
        User current = schoolScopeService.requireCurrentUser();
        if (dto.getCategory() == RequestCategory.GPA_VIEW) {
            return requestGpaAccess();
        }
        UserRequest request = new UserRequest();
        request.setFromUser(current);
        request.setSubject(dto.getSubject().trim());
        request.setBody(dto.getBody().trim());
        request.setCategory(dto.getCategory());
        request.setStatus(RequestStatus.OPEN);
        return UserRequestResponse.from(requestRepository.save(request));
    }

    @Override
    public UserRequestResponse requestGpaAccess() {
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role != RoleName.STUDENT) {
            throw new AccessDeniedException("Only students can request GPA access");
        }
        if (hasApprovedGpaAccess(current.getUuid())) {
            throw new IllegalArgumentException("GPA access is already approved");
        }
        UserRequest pending = latestGpaRequest(current.getUuid());
        if (pending != null && PENDING_STATUSES.contains(pending.getStatus())) {
            throw new IllegalArgumentException("You already have a pending GPA request");
        }

        UserRequest request = new UserRequest();
        request.setFromUser(current);
        request.setSubject("Request to view scores and GPA");
        request.setBody("I would like permission to view my exam scores and GPA.");
        request.setCategory(RequestCategory.GPA_VIEW);
        request.setStatus(RequestStatus.OPEN);
        return UserRequestResponse.from(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequestResponse> listMine() {
        User current = schoolScopeService.requireCurrentUser();
        return requestRepository.findDetailedByFromUser(current.getUuid()).stream()
                .map(UserRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequestResponse> listAll(RequestStatus status) {
        requireRequestWrite();
        boolean filterStatus = status != null;
        return requestRepository
                .findDetailedByStatus(filterStatus, filterStatus ? status : RequestStatus.OPEN)
                .stream()
                .filter(this::inSchoolScope)
                .map(UserRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserRequestResponse getById(UUID id) {
        UserRequest request = find(id);
        User current = schoolScopeService.requireCurrentUser();
        boolean owner = request.getFromUser() != null && current.getUuid().equals(request.getFromUser().getUuid());
        if (!owner && !hasAuthority("REQUEST_READ")) {
            throw new AccessDeniedException("You can only view your own requests");
        }
        if (!owner) {
            assertInScope(request);
        }
        return UserRequestResponse.from(request);
    }

    @Override
    public UserRequestResponse reply(UUID id, UserRequestReplyDto reply) {
        requireRequestWrite();
        UserRequest request = find(id);
        assertInScope(request);
        if (request.getCategory() == RequestCategory.GPA_VIEW
                && (reply.getStatus() == RequestStatus.APPROVED
                        || reply.getStatus() == RequestStatus.REJECTED
                        || reply.getStatus() == RequestStatus.CLOSED
                        || request.getStatus() == RequestStatus.APPROVED)) {
            requireGpaApprover();
        }
        User handler = schoolScopeService.requireCurrentUser();
        request.setStatus(reply.getStatus());
        request.setAdminReply(blankToNull(reply.getAdminReply()));
        request.setHandledBy(handler);
        UserRequest saved = requestRepository.save(request);
        syncGpaGrant(saved, handler);
        return UserRequestResponse.from(saved);
    }

    @Override
    public UserRequestResponse approve(UUID id, String adminReply) {
        requireGpaApprover();
        UserRequest request = find(id);
        assertInScope(request);
        if (request.getCategory() != RequestCategory.GPA_VIEW) {
            throw new IllegalArgumentException("Only GPA view requests can be approved this way");
        }
        if (request.getStatus() == RequestStatus.APPROVED) {
            syncGpaGrant(request, schoolScopeService.requireCurrentUser());
            return UserRequestResponse.from(request);
        }
        if (!PENDING_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Only open or in-progress GPA requests can be approved");
        }
        User handler = schoolScopeService.requireCurrentUser();
        request.setStatus(RequestStatus.APPROVED);
        request.setAdminReply(blankToNull(adminReply) != null
                ? blankToNull(adminReply)
                : "GPA access approved. You may now view your scores and GPA.");
        request.setHandledBy(handler);
        UserRequest saved = requestRepository.save(request);
        syncGpaGrant(saved, handler);
        return UserRequestResponse.from(saved);
    }

    @Override
    public UserRequestResponse reject(UUID id, String adminReply) {
        requireGpaApprover();
        UserRequest request = find(id);
        assertInScope(request);
        if (request.getCategory() != RequestCategory.GPA_VIEW) {
            throw new IllegalArgumentException("Only GPA view requests can be rejected this way");
        }
        if (!PENDING_STATUSES.contains(request.getStatus()) && request.getStatus() != RequestStatus.APPROVED) {
            throw new IllegalArgumentException("This GPA request cannot be rejected");
        }
        User handler = schoolScopeService.requireCurrentUser();
        request.setStatus(RequestStatus.REJECTED);
        request.setAdminReply(blankToNull(adminReply) != null
                ? blankToNull(adminReply)
                : "GPA access request was rejected.");
        request.setHandledBy(handler);
        UserRequest saved = requestRepository.save(request);
        syncGpaGrant(saved, handler);
        return UserRequestResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GpaAccessStatus gpaAccessForCurrentUser() {
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role != RoleName.STUDENT) {
            return GpaAccessStatus.builder()
                    .canViewGpa(true)
                    .canRequest(false)
                    .message("Staff can view GPA without a request")
                    .build();
        }
        return gpaAccessForStudent(current.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedGpaAccess(UUID studentUuid) {
        return requestRepository.existsByFromUser_UuidAndCategoryAndStatus(
                studentUuid, RequestCategory.GPA_VIEW, RequestStatus.APPROVED);
    }

    private GpaAccessStatus gpaAccessForStudent(UUID studentUuid) {
        if (hasApprovedGpaAccess(studentUuid)) {
            UserRequest approved = latestGpaRequest(studentUuid);
            return GpaAccessStatus.builder()
                    .canViewGpa(true)
                    .canRequest(false)
                    .status(RequestStatus.APPROVED)
                    .requestUuid(approved != null ? approved.getUuid() : null)
                    .message("GPA access approved")
                    .build();
        }
        UserRequest latest = latestGpaRequest(studentUuid);
        if (latest == null) {
            return GpaAccessStatus.builder()
                    .canViewGpa(false)
                    .canRequest(true)
                    .message("Request approval from the principal or admin to view your scores and GPA")
                    .build();
        }
        if (PENDING_STATUSES.contains(latest.getStatus())) {
            return GpaAccessStatus.builder()
                    .canViewGpa(false)
                    .canRequest(false)
                    .status(latest.getStatus())
                    .requestUuid(latest.getUuid())
                    .message("Your request is waiting for principal or admin approval")
                    .build();
        }
        if (latest.getStatus() == RequestStatus.REJECTED) {
            return GpaAccessStatus.builder()
                    .canViewGpa(false)
                    .canRequest(true)
                    .status(RequestStatus.REJECTED)
                    .requestUuid(latest.getUuid())
                    .message(latest.getAdminReply() != null
                            ? latest.getAdminReply()
                            : "Your request was rejected. You may submit a new request.")
                    .build();
        }
        return GpaAccessStatus.builder()
                .canViewGpa(false)
                .canRequest(true)
                .status(latest.getStatus())
                .requestUuid(latest.getUuid())
                .message("Request approval from the principal or admin to view your scores and GPA")
                .build();
    }

    private UserRequest latestGpaRequest(UUID userUuid) {
        List<UserRequest> requests = requestRepository.findDetailedByFromUserAndCategory(
                userUuid, RequestCategory.GPA_VIEW);
        return requests.isEmpty() ? null : requests.get(0);
    }

    private void syncGpaGrant(UserRequest request, User handler) {
        if (request.getCategory() != RequestCategory.GPA_VIEW || request.getFromUser() == null) {
            return;
        }
        boolean approved = request.getStatus() == RequestStatus.APPROVED;
        assessmentClient.setGpaAccess(
                request.getFromUser().getUuid(),
                approved,
                handler != null ? handler.getUuid() : null);
    }

    private UserRequest find(UUID id) {
        return requestRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Request not found: " + id));
    }

    private boolean inSchoolScope(UserRequest request) {
        if (request.getFromUser() == null || request.getFromUser().getSchool() == null) {
            return true;
        }
        return schoolScopeService.scopedSchoolUuid().isEmpty()
                || schoolScopeService.scopedSchoolUuid().get()
                        .equals(request.getFromUser().getSchool().getUuid());
    }

    private void assertInScope(UserRequest request) {
        if (!inSchoolScope(request)) {
            throw new AccessDeniedException("You can only access requests from your school");
        }
    }

    private void requireRequestWrite() {
        if (!hasAuthority("REQUEST_WRITE")) {
            throw new AccessDeniedException("REQUEST_WRITE required");
        }
    }

    private void requireGpaApprover() {
        requireRequestWrite();
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        if (role == null || !GPA_APPROVERS.contains(role)) {
            throw new AccessDeniedException("Only principal or admin can approve GPA requests");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
