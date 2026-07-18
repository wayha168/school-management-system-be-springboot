package com.project.school_management.service.permission;

import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.permission.PermissionAssignRequest;
import com.project.school_management.dto.permission.PermissionItemResponse;
import com.project.school_management.dto.permission.RolePermissionRequest;
import com.project.school_management.enums.RoleName;

public interface PermissionService {

    List<PermissionItemResponse> getAll();

    List<String> getPermissionsForRole(RoleName roleName);

    List<String> getPermissionsForRole(UUID roleUuid);

    void replaceRolePermissions(RolePermissionRequest request);

    void assign(PermissionAssignRequest request);

    void revoke(UUID roleUuid, String permission);

    void seedDefaultsIfEmpty();
}
