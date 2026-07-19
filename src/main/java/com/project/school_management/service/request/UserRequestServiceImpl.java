package com.project.school_management.service.request;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.request.UserRequestDto;
import com.project.school_management.dto.request.UserRequestReplyDto;
import com.project.school_management.dto.request.UserRequestResponse;
import com.project.school_management.entities.User;
import com.project.school_management.entities.UserRequest;
import com.project.school_management.enums.RequestStatus;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.UserRequestRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class UserRequestServiceImpl implements UserRequestService {

    private final UserRequestRepository requestRepository;
    private final SchoolScopeService schoolScopeService;

    public UserRequestServiceImpl(UserRequestRepository requestRepository, SchoolScopeService schoolScopeService) {
        this.requestRepository = requestRepository;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    public UserRequestResponse create(UserRequestDto dto) {
        User current = schoolScopeService.requireCurrentUser();
        UserRequest request = new UserRequest();
        request.setFromUser(current);
        request.setSubject(dto.getSubject().trim());
        request.setBody(dto.getBody().trim());
        request.setCategory(dto.getCategory());
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
        User handler = schoolScopeService.requireCurrentUser();
        request.setStatus(reply.getStatus());
        request.setAdminReply(reply.getAdminReply() == null || reply.getAdminReply().isBlank()
                ? null
                : reply.getAdminReply().trim());
        request.setHandledBy(handler);
        return UserRequestResponse.from(requestRepository.save(request));
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
