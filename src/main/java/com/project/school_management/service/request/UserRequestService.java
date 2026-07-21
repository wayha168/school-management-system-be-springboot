package com.project.school_management.service.request;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.request.GpaAccessStatus;
import com.project.school_management.dto.request.UserRequestDto;
import com.project.school_management.dto.request.UserRequestReplyDto;
import com.project.school_management.dto.request.UserRequestResponse;
import com.project.school_management.enums.RequestStatus;

public interface UserRequestService {

    UserRequestResponse create(UserRequestDto request);

    UserRequestResponse requestGpaAccess();

    List<UserRequestResponse> listMine();

    List<UserRequestResponse> listAll(RequestStatus status);

    UserRequestResponse getById(UUID id);

    UserRequestResponse reply(UUID id, UserRequestReplyDto reply);

    UserRequestResponse approve(UUID id, String adminReply);

    UserRequestResponse reject(UUID id, String adminReply);

    GpaAccessStatus gpaAccessForCurrentUser();

    boolean hasApprovedGpaAccess(UUID studentUuid);
}
