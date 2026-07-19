package com.project.school_management.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private UUID uuid;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private RoleName role;
    private UUID roleUuid;
    private UUID schoolUuid;
    private String schoolName;
    private List<UserClassItem> classes;
    private String classNames;
    private String grade;
    private String room;
    private boolean hasProfileImage;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        List<UserClassItem> classes = user.getSchoolClasses() == null
                ? List.of()
                : user.getSchoolClasses().stream().map(UserClassItem::from).toList();

        String classNames = classes.isEmpty()
                ? null
                : classes.stream()
                        .map(c -> c.getName() + " (" + (c.getGenerationCode() != null ? c.getGenerationCode() : "?") + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(null);

        String grade = user.getGrade();
        if ((grade == null || grade.isBlank()) && !classes.isEmpty()) {
            grade = classes.stream()
                    .map(UserClassItem::getGrade)
                    .filter(g -> g != null && !g.isBlank())
                    .findFirst()
                    .orElse(null);
        }

        UUID id = user.getUuid();
        boolean hasProfileImage = user.hasProfileImage();
        String[] nameParts = splitName(user.getName());

        return UserResponse.builder()
                .uuid(id)
                .name(user.getName())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .roleUuid(user.getRole() != null ? user.getRole().getUuid() : null)
                .schoolUuid(user.getSchool() != null ? user.getSchool().getUuid() : null)
                .schoolName(user.getSchool() != null ? user.getSchool().getName() : null)
                .classes(classes)
                .classNames(classNames)
                .grade(grade)
                .room(user.getRoom())
                .hasProfileImage(hasProfileImage)
                .profileImageUrl(hasProfileImage && id != null ? "/admin/users/" + id + "/avatar" : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /** Split stored full name into first + remaining last name. */
    public static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", ""};
        }
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] {trimmed, ""};
        }
        return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    public static String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }
}
