package com.project.school_management.service.role;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.role.RoleRequest;
import com.project.school_management.dto.role.RoleResponse;
import com.project.school_management.entities.Role;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.RoleRepository;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        }
        Role role = new Role(request.getName(), request.getDescription());
        return RoleResponse.from(roleRepository.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        return RoleResponse.from(findRole(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    @Override
    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = findRole(id);
        roleRepository.findByName(request.getName())
                .filter(existing -> !existing.getUuid().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Role already exists: " + request.getName());
                });
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return RoleResponse.from(roleRepository.save(role));
    }

    @Override
    public void delete(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new ExceptionNotFound("Role not found: " + id);
        }
        roleRepository.deleteById(id);
    }

    private Role findRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("Role not found: " + id));
    }
}
