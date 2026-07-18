package com.project.school_management.service.user;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    UserResponse update(UUID id, UserUpdateRequest request);

    void delete(UUID id);
}
