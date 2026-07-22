package com.project.assignment.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record CallerContext(
        UUID userUuid,
        String email,
        String role,
        Set<String> authorities,
        UUID schoolUuid) {

    public boolean hasAuthority(String authority) {
        return authorities != null && authorities.contains(authority);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(role) || hasAuthority("ROLE_STUDENT");
    }

    public boolean isTeacher() {
        return "TEACHER".equalsIgnoreCase(role) || hasAuthority("ROLE_TEACHER");
    }

    public boolean canManageClassroom() {
        return isTeacher()
                || hasAuthority("ROLE_PRINCIPAL")
                || hasAuthority("ROLE_ADMIN")
                || hasAuthority("ROLE_SUPERADMIN")
                || "PRINCIPAL".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "SUPERADMIN".equalsIgnoreCase(role)
                || hasAuthority("ASSIGNMENT_WRITE")
                || hasAuthority("MEETING_WRITE");
    }

    public static CallerContext empty() {
        return new CallerContext(null, null, null, Collections.emptySet(), null);
    }
}
