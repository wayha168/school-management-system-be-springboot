package com.project.school_management.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.repository.UserRepository;

/**
 * Restricts school-bound roles (e.g. TEACHER) to their own school.
 * SUPERADMIN / ADMIN and anonymous public callers are not school-scoped.
 */
@Service
public class SchoolScopeService {

    private static final Set<RoleName> GLOBAL_ROLES = Set.of(RoleName.SUPERADMIN, RoleName.ADMIN);

    private final UserRepository userRepository;

    public SchoolScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isGlobalAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            return true;
        }
        User user = findCurrentUser(auth);
        RoleName role = user.getRole() != null ? user.getRole().getName() : null;
        return role != null && GLOBAL_ROLES.contains(role);
    }

    /** Empty = no school filter (global). Present = only this school UUID. */
    @Transactional(readOnly = true)
    public Optional<UUID> scopedSchoolUuid() {
        if (isGlobalAccess()) {
            return Optional.empty();
        }
        User user = requireCurrentUser();
        if (user.getSchool() == null || user.getSchool().getUuid() == null) {
            throw new AccessDeniedException("Your account is not assigned to a school");
        }
        return Optional.of(user.getSchool().getUuid());
    }

    @Transactional(readOnly = true)
    public void assertSchoolAccess(UUID schoolUuid) {
        if (schoolUuid == null) {
            throw new AccessDeniedException("School is required");
        }
        scopedSchoolUuid().ifPresent(mine -> {
            if (!mine.equals(schoolUuid)) {
                throw new AccessDeniedException("You can only access your own school");
            }
        });
    }

    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            throw new AccessDeniedException("Authentication required");
        }
        return findCurrentUser(auth);
    }

    private User findCurrentUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findDetailedByEmail(email)
                .orElseThrow(() -> new UserNotFound("Account not found: " + email));
    }

    private static boolean isAnonymous(Authentication auth) {
        Object principal = auth.getPrincipal();
        return principal == null
                || "anonymousUser".equals(principal)
                || "anonymousUser".equals(String.valueOf(principal));
    }
}
