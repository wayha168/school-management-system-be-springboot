package com.project.school_management.dto.user;

import java.util.List;
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
    private UUID schoolUuid;
    private String schoolName;
    private UUID classUuid;
    private String className;
    private List<String> permissions;

    public static DataUser from(User user, List<String> permissions) {
        RoleName roleName = user.getRole() != null ? user.getRole().getName() : null;

        return DataUser.builder()
                .uuid(user.getUuid())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .roleLabel(roleName != null ? roleName.name() : "UNKNOWN")
                .schoolUuid(user.getSchool() != null ? user.getSchool().getUuid() : null)
                .schoolName(user.getSchool() != null ? user.getSchool().getName() : null)
                .classUuid(user.getSchoolClass() != null ? user.getSchoolClass().getUuid() : null)
                .className(user.getSchoolClass() != null ? user.getSchoolClass().getName() : null)
                .permissions(permissions != null ? permissions : List.of())
                .build();
    }
}
