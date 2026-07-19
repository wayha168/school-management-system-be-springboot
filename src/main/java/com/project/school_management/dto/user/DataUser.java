package com.project.school_management.dto.user;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DataUser {

    private UUID uuid;
    private String name;
    private String email;
    private RoleName role;
    private String roleLabel;
    private String roleDisplay;
    private String grade;
    private String room;
    private UUID schoolUuid;
    private String schoolName;
    private List<UserClassItem> classes;
    private String classNames;
    private boolean hasProfileImage;
    private String profileImageUrl;
    private List<String> permissions;

    public static DataUser from(User user, List<String> permissions) {
        RoleName roleName = user.getRole() != null ? user.getRole().getName() : null;
        List<UserClassItem> classes = user.getSchoolClasses() == null
                ? List.of()
                : user.getSchoolClasses().stream().map(UserClassItem::from).toList();

        String classNames = classes.isEmpty()
                ? null
                : classes.stream()
                        .map(c -> {
                            String gen = c.getGenerationCode() != null
                                    ? c.getGenerationCode()
                                    : (c.getGeneration() != null ? "G" + c.getGeneration() : null);
                            return gen == null ? c.getName() : c.getName() + " · " + gen;
                        })
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(null);

        UUID id = user.getUuid();
        boolean hasProfileImage = user.hasProfileImage();

        return DataUser.builder()
                .uuid(id)
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .roleLabel(roleName != null ? roleName.name() : "UNKNOWN")
                .roleDisplay(displayRole(roleName))
                .grade(user.getGrade())
                .room(user.getRoom())
                .schoolUuid(user.getSchool() != null ? user.getSchool().getUuid() : null)
                .schoolName(user.getSchool() != null ? user.getSchool().getName() : null)
                .classes(classes)
                .classNames(classNames)
                .hasProfileImage(hasProfileImage)
                .profileImageUrl(hasProfileImage && id != null ? "/admin/users/" + id + "/avatar" : null)
                .permissions(permissions != null ? permissions : List.of())
                .build();
    }

    public static String displayRole(RoleName role) {
        if (role == null) {
            return "Unknown";
        }
        return switch (role) {
            case SUPERADMIN -> "Super Admin";
            case ADMIN -> "Admin";
            case PRINCIPAL -> "Principal";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
            case STAFF -> "Staff";
        };
    }

    public String roleCss() {
        if (role == null) {
            return "unknown";
        }
        return role.name().toLowerCase(Locale.ROOT);
    }
}
