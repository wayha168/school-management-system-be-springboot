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
    private boolean hasLogo;
    private boolean hasBanner;
    private String logoUrl;
    private String bannerUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SchoolResponse from(SchoolMag school) {
        UUID id = school.getUuid();
        boolean hasLogo = school.hasLogo();
        boolean hasBanner = school.hasBanner();
        return SchoolResponse.builder()
                .uuid(id)
                .name(school.getName())
                .description(school.getDescription())
                .address(school.getAddress())
                .phone(school.getPhone())
                .email(school.getEmail())
                .website(school.getWebsite())
                .hasLogo(hasLogo)
                .hasBanner(hasBanner)
                .logoUrl(hasLogo && id != null ? "/api/v1/schools/" + id + "/logo" : null)
                .bannerUrl(hasBanner && id != null ? "/api/v1/schools/" + id + "/banner" : null)
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
