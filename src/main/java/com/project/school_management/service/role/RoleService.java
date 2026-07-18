package com.project.school_management.service.role;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.role.RoleRequest;
import com.project.school_management.dto.role.RoleResponse;

public interface RoleService {

    RoleResponse create(RoleRequest request);

    RoleResponse getById(UUID id);

    List<RoleResponse> getAll();

    RoleResponse update(UUID id, RoleRequest request);

    void delete(UUID id);
}
