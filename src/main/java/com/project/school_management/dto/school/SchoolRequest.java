package com.project.school_management.dto.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchoolRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String address;

    @NotBlank
    private String phone;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String website;

    /** Optional base64 / data-URL for API clients (form uploads use multipart instead). */
    private String logoBase64;

    /** Optional base64 / data-URL for API clients (form uploads use multipart instead). */
    private String bannerBase64;
}
