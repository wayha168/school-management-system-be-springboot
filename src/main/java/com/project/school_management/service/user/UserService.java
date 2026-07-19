package com.project.school_management.service.user;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse create(UserRequest request, MultipartFile profileImage);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    UserResponse update(UUID id, UserUpdateRequest request);

    UserResponse update(UUID id, UserUpdateRequest request, MultipartFile profileImage);

    void delete(UUID id);

    DataUser getAccountByEmail(String email);

    SchoolImage getProfileImage(UUID id);
}
