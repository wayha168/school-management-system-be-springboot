package com.project.school_management.security;

import java.util.EnumSet;
import java.util.Set;

import com.project.school_management.enums.Permission;
import com.project.school_management.enums.RoleName;

public final class RolePermissions {

    private RolePermissions() {
    }

    public static Set<Permission> forRole(RoleName role) {
        return switch (role) {
            case SUPERADMIN -> EnumSet.allOf(Permission.class);
            case ADMIN -> EnumSet.of(
                    Permission.SCHOOL_READ,
                    Permission.SCHOOL_WRITE,
                    Permission.SCHOOL_EDIT,
                    Permission.SCHOOL_DELETE,
                    Permission.USER_READ,
                    Permission.USER_WRITE,
                    Permission.USER_EDIT,
                    Permission.USER_DELETE,
                    Permission.CLASS_READ,
                    Permission.CLASS_WRITE,
                    Permission.CLASS_EDIT,
                    Permission.CLASS_DELETE,
                    Permission.ROLES_READ,
                    Permission.ROLES_WRITE,
                    Permission.STUDENT_READ,
                    Permission.STUDENT_WRITE,
                    Permission.STUDENT_EDIT,
                    Permission.STUDENT_DELETE,
                    Permission.TEACHER_READ,
                    Permission.TEACHER_WRITE,
                    Permission.TEACHER_EDIT,
                    Permission.TEACHER_DELETE,
                    Permission.STAFF_READ,
                    Permission.STAFF_WRITE,
                    Permission.STAFF_EDIT,
                    Permission.STAFF_DELETE,
                    Permission.SCORE_READ,
                    Permission.SCORE_WRITE,
                    Permission.FINANCE_READ,
                    Permission.FINANCE_WRITE,
                    Permission.REQUEST_READ,
                    Permission.REQUEST_WRITE,
                    Permission.ATTENDANCE_READ,
                    Permission.ATTENDANCE_WRITE);
            case PRINCIPAL -> EnumSet.of(
                    Permission.SCHOOL_READ,
                    Permission.USER_READ,
                    Permission.USER_WRITE,
                    Permission.USER_EDIT,
                    Permission.CLASS_READ,
                    Permission.CLASS_WRITE,
                    Permission.CLASS_EDIT,
                    Permission.ROLES_READ,
                    Permission.STUDENT_READ,
                    Permission.STUDENT_WRITE,
                    Permission.TEACHER_READ,
                    Permission.STAFF_READ,
                    Permission.SCORE_READ,
                    Permission.SCORE_WRITE,
                    Permission.FINANCE_READ,
                    Permission.REQUEST_READ,
                    Permission.REQUEST_WRITE,
                    Permission.ATTENDANCE_READ);
            case TEACHER -> EnumSet.of(
                    Permission.SCHOOL_READ,
                    Permission.USER_READ,
                    Permission.CLASS_READ,
                    Permission.STUDENT_READ,
                    Permission.SCORE_READ,
                    Permission.SCORE_WRITE,
                    Permission.REQUEST_READ,
                    Permission.ATTENDANCE_READ,
                    Permission.ATTENDANCE_WRITE);
            case STAFF -> EnumSet.of(
                    Permission.SCHOOL_READ,
                    Permission.USER_READ,
                    Permission.CLASS_READ,
                    Permission.ROLES_READ,
                    Permission.STAFF_READ,
                    Permission.FINANCE_READ,
                    Permission.FINANCE_WRITE,
                    Permission.REQUEST_READ,
                    Permission.REQUEST_WRITE,
                    Permission.ATTENDANCE_READ);
            case STUDENT -> EnumSet.of(
                    Permission.SCHOOL_READ,
                    Permission.CLASS_READ,
                    Permission.SCORE_READ,
                    Permission.REQUEST_READ,
                    Permission.ATTENDANCE_READ);
        };
    }
}
