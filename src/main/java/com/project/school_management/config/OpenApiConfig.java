package com.project.school_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI schoolManagementOpenApi() {
        final String scheme = "basicAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("School Management API")
                        .description("CRUD APIs for schools, classes, users, and roles. "
                                + "Use Authorize with admin email/password (HTTP Basic).")
                        .version("v1")
                        .contact(new Contact().name("School Management")))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(
                        scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}
