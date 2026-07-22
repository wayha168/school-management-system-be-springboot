package com.project.school_management.enums;

public enum Permission {

    //School permissions
    SCHOOL_READ,
    SCHOOL_WRITE,
    SCHOOL_EDIT,
    SCHOOL_DELETE,

    //User permissions
    USER_READ,
    USER_WRITE,
    USER_EDIT,
    USER_DELETE,

    //Class permissions
    CLASS_READ,
    CLASS_WRITE,
    CLASS_EDIT,
    CLASS_DELETE,

    //Roles permissions
    ROLES_READ,
    ROLES_WRITE,
    ROLES_EDIT,
    ROLES_DELETE,

    //Student permissions
    STUDENT_READ,
    STUDENT_WRITE,
    STUDENT_EDIT,
    STUDENT_DELETE,

    //Teacher permissions
    TEACHER_READ,
    TEACHER_WRITE,
    TEACHER_EDIT,
    TEACHER_DELETE,

    //Parent permissions
    STAFF_READ,
    STAFF_WRITE,
    STAFF_EDIT,
    STAFF_DELETE,

    //Score permissions
    SCORE_READ,
    SCORE_WRITE,

    //Finance
    FINANCE_READ,
    FINANCE_WRITE,

    //Requests / complaints
    REQUEST_READ,
    REQUEST_WRITE,

    //Attendance
    ATTENDANCE_READ,
    ATTENDANCE_WRITE,

    //Classroom assignments / meetings
    ASSIGNMENT_READ,
    ASSIGNMENT_WRITE,
    MEETING_READ,
    MEETING_WRITE,
}
