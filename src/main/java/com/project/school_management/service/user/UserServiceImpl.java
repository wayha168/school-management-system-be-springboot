package com.project.school_management.service.user;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;
import com.project.school_management.entities.Role;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.repository.RoleRepository;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.service.permission.PermissionService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final SchoolScopeService schoolScopeService;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository,
            PasswordEncoder passwordEncoder,
            PermissionService permissionService,
            SchoolScopeService schoolScopeService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    public UserResponse create(UserRequest request) {
        return create(request, null);
    }

    @Override
    public UserResponse create(UserRequest request, MultipartFile profileImage) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(findRole(request.getRoleUuid()));
        user.setSchool(findSchool(request.getSchoolUuid()));
        List<SchoolClass> classes = resolveClasses(request.getClassUuids());
        user.setSchoolClasses(classes);
        String grade = blankToNull(request.getGrade());
        if (grade == null && !classes.isEmpty()) {
            grade = classes.stream()
                    .map(SchoolClass::getGrade)
                    .filter(g -> g != null && !g.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        user.setGrade(grade);
        user.setRoom(blankToNull(request.getRoom()));
        user.setSalary(salaryForRole(user.getRole() != null ? user.getRole().getName() : null, request.getSalary()));
        applyProfileImage(user, profileImage, false);
        User saved = userRepository.save(user);
        return UserResponse.from(findUser(saved.getUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = findUser(id);
        if (user.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
        }
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return schoolScopeService.scopedSchoolUuid()
                .map(schoolUuid -> userRepository.findDetailedBySchoolUuid(schoolUuid))
                .orElseGet(userRepository::findAllDetailed)
                .stream()
                .map(UserResponse::from)
                .sorted(java.util.Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public UserResponse update(UUID id, UserUpdateRequest request) {
        return update(id, request, null);
    }

    @Override
    public UserResponse update(UUID id, UserUpdateRequest request, MultipartFile profileImage) {
        User user = findUser(id);
        if (user.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
        }
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());

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
        List<SchoolClass> classes = resolveClasses(request.getClassUuids());
        user.setSchoolClasses(classes);
        String grade = blankToNull(request.getGrade());
        if (grade == null && !classes.isEmpty()) {
            grade = classes.stream()
                    .map(SchoolClass::getGrade)
                    .filter(g -> g != null && !g.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        user.setGrade(grade);
        user.setRoom(blankToNull(request.getRoom()));
        user.setSalary(salaryForRole(user.getRole() != null ? user.getRole().getName() : null, request.getSalary()));
        applyProfileImage(user, profileImage, true);
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

    @Override
    @Transactional(readOnly = true)
    public SchoolImage getProfileImage(UUID id) {
        User user = findUser(id);
        if (user.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
        }
        if (!user.hasProfileImage()) {
            throw new ExceptionNotFound("Profile image not found for user: " + id);
        }
        String contentType = user.getProfileImageContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/png";
        }
        return new SchoolImage(user.getProfileImageData(), contentType);
    }

    private void applyProfileImage(User user, MultipartFile file, boolean keepExisting) {
        if (file != null && !file.isEmpty()) {
            String contentType = normalizeContentType(file.getContentType());
            validateContentType(contentType);
            try {
                byte[] data = file.getBytes();
                validateSize(data.length);
                user.setProfileImageData(data);
                user.setProfileImageContentType(contentType);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to read profile image", ex);
            }
        } else if (!keepExisting) {
            user.setProfileImageData(null);
            user.setProfileImageContentType(null);
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed");
        }
    }

    private static void validateSize(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be 5MB or smaller");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/png";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
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

    private List<SchoolClass> resolveClasses(List<UUID> classUuids) {
        if (classUuids == null || classUuids.isEmpty()) {
            return new ArrayList<>();
        }
        List<SchoolClass> classes = new ArrayList<>();
        for (UUID classUuid : classUuids) {
            if (classUuid == null) {
                continue;
            }
            classes.add(schoolClassRepository.findById(classUuid)
                    .orElseThrow(() -> new ExceptionNotFound("Class not found: " + classUuid)));
        }
        return classes;
    }

    private static final Set<RoleName> SALARY_ROLES = EnumSet.of(
            RoleName.TEACHER,
            RoleName.STAFF,
            RoleName.PRINCIPAL,
            RoleName.ADMIN,
            RoleName.SUPERADMIN);

    private static BigDecimal salaryForRole(RoleName role, BigDecimal salary) {
        if (role == null || !SALARY_ROLES.contains(role)) {
            return null;
        }
        if (salary == null) {
            return null;
        }
        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        return salary;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
