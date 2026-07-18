package com.project.school_management.service.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.permission.PermissionAssignRequest;
import com.project.school_management.dto.permission.PermissionItemResponse;
import com.project.school_management.dto.permission.RolePermissionRequest;
import com.project.school_management.entities.Role;
import com.project.school_management.entities.RolePermission;
import com.project.school_management.enums.Permission;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.repository.RolePermissionRepository;
import com.project.school_management.repository.RoleRepository;
import com.project.school_management.security.RolePermissions;

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;

    public PermissionServiceImpl(
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionItemResponse> getAll() {
        Map<String, List<String>> rolesByPermission = new LinkedHashMap<>();
        for (Permission permission : Permission.values()) {
            rolesByPermission.put(permission.name(), new ArrayList<>());
        }

        for (Role role : roleRepository.findAll()) {
            List<String> permissions = getPermissionsForRole(role.getUuid());
            for (String permission : permissions) {
                rolesByPermission.computeIfAbsent(permission, key -> new ArrayList<>()).add(role.getName().name());
            }
        }

        List<PermissionItemResponse> items = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            String code = permission.name();
            String[] parts = code.split("_", 2);
            items.add(PermissionItemResponse.builder()
                    .code(code)
                    .module(parts.length > 0 ? parts[0] : code)
                    .action(parts.length > 1 ? parts[1] : "ALL")
                    .status("Active")
                    .roles(rolesByPermission.getOrDefault(code, List.of()))
                    .build());
        }
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPermissionsForRole(RoleName roleName) {
        List<RolePermission> stored = rolePermissionRepository.findByRole_Name(roleName);
        if (!stored.isEmpty()) {
            return stored.stream().map(RolePermission::getPermission).sorted().toList();
        }
        return RolePermissions.forRole(roleName).stream().map(Enum::name).sorted().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPermissionsForRole(UUID roleUuid) {
        Role role = findRole(roleUuid);
        List<RolePermission> stored = rolePermissionRepository.findByRoleUuid(roleUuid);
        if (!stored.isEmpty()) {
            return stored.stream().map(RolePermission::getPermission).sorted().toList();
        }
        return RolePermissions.forRole(role.getName()).stream().map(Enum::name).sorted().toList();
    }

    @Override
    public void replaceRolePermissions(RolePermissionRequest request) {
        Role role = findRole(request.getRoleUuid());
        validatePermissions(request.getPermissions());
        rolePermissionRepository.deleteByRoleUuid(role.getUuid());
        for (String permission : request.getPermissions()) {
            rolePermissionRepository.save(new RolePermission(role, permission));
        }
    }

    @Override
    public void assign(PermissionAssignRequest request) {
        Role role = findRole(request.getRoleUuid());
        String permission = normalizePermission(request.getPermission());
        if (rolePermissionRepository.existsByRoleUuidAndPermission(role.getUuid(), permission)) {
            return;
        }
        // Materialize defaults first if empty so assign doesn't lose them
        if (rolePermissionRepository.findByRoleUuid(role.getUuid()).isEmpty()) {
            for (String defaultPermission : RolePermissions.forRole(role.getName()).stream().map(Enum::name).toList()) {
                rolePermissionRepository.save(new RolePermission(role, defaultPermission));
            }
            if (rolePermissionRepository.existsByRoleUuidAndPermission(role.getUuid(), permission)) {
                return;
            }
        }
        rolePermissionRepository.save(new RolePermission(role, permission));
    }

    @Override
    public void revoke(UUID roleUuid, String permission) {
        findRole(roleUuid);
        String normalized = normalizePermission(permission);
        if (rolePermissionRepository.findByRoleUuid(roleUuid).isEmpty()) {
            Role role = findRole(roleUuid);
            for (String defaultPermission : RolePermissions.forRole(role.getName()).stream().map(Enum::name).toList()) {
                if (!defaultPermission.equals(normalized)) {
                    rolePermissionRepository.save(new RolePermission(role, defaultPermission));
                }
            }
            return;
        }
        rolePermissionRepository.deleteByRoleUuidAndPermission(roleUuid, normalized);
    }

    @Override
    public void seedDefaultsIfEmpty() {
        if (rolePermissionRepository.count() > 0) {
            return;
        }
        for (Role role : roleRepository.findAll()) {
            for (Permission permission : RolePermissions.forRole(role.getName())) {
                rolePermissionRepository.save(new RolePermission(role, permission.name()));
            }
        }
    }

    private Role findRole(UUID roleUuid) {
        return roleRepository.findById(roleUuid)
                .orElseThrow(() -> new ExceptionNotFound("Role not found: " + roleUuid));
    }

    private void validatePermissions(List<String> permissions) {
        for (String permission : permissions) {
            normalizePermission(permission);
        }
    }

    private String normalizePermission(String permission) {
        try {
            return Permission.valueOf(permission.trim().toUpperCase(Locale.ROOT)).name();
        } catch (Exception ex) {
            throw new ErrorRuntime("Unknown permission: " + permission);
        }
    }
}
