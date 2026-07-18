package com.project.school_management.service.user;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;
import com.project.school_management.entities.Role;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.User;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.repository.RoleRepository;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.service.permission.PermissionService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository,
            PasswordEncoder passwordEncoder,
            PermissionService permissionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    @Override
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(findRole(request.getRoleUuid()));
        user.setSchool(findSchool(request.getSchoolUuid()));
        user.setSchoolClass(resolveClass(request.getClassUuid()));
        user.setGrade(blankToNull(request.getGrade()));
        user.setRoom(blankToNull(request.getRoom()));
        User saved = userRepository.save(user);
        return UserResponse.from(findUser(saved.getUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllDetailed().stream().map(UserResponse::from).toList();
    }

    @Override
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findUser(id);

        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getUuid().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already exists: " + request.getEmail());
                });

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(findRole(request.getRoleUuid()));
        user.setSchool(findSchool(request.getSchoolUuid()));
        user.setSchoolClass(resolveClass(request.getClassUuid()));
        user.setGrade(blankToNull(request.getGrade()));
        user.setRoom(blankToNull(request.getRoom()));
        userRepository.save(user);
        return UserResponse.from(findUser(id));
    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFound("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DataUser getAccountByEmail(String email) {
        User user = userRepository.findDetailedByEmail(email)
                .orElseThrow(() -> new UserNotFound("Account not found: " + email));
        List<String> permissions = user.getRole() == null
                ? List.of()
                : permissionService.getPermissionsForRole(user.getRole().getName());
        return DataUser.from(user, permissions);
    }

    private User findUser(UUID id) {
        return userRepository.findDetailedById(id)
                .orElseThrow(() -> new UserNotFound("User not found: " + id));
    }

    private Role findRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("Role not found: " + id));
    }

    private SchoolMag findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("School not found: " + id));
    }

    private SchoolClass resolveClass(UUID classUuid) {
        if (classUuid == null) {
            return null;
        }
        return schoolClassRepository.findById(classUuid)
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + classUuid));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
