package com.project.school_management.dto.school;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.school_management.entities.SchoolMag;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolResponse {

    private UUID uuid;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String logo;
    private String banner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolResponse from(SchoolMag school) {
        return SchoolResponse.builder()
                .uuid(school.getUuid())
                .name(school.getName())
                .description(school.getDescription())
                .address(school.getAddress())
                .phone(school.getPhone())
                .email(school.getEmail())
                .website(school.getWebsite())
                .logo(school.getLogo())
                .banner(school.getBanner())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
