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

        private static final String BEARER = "bearerAuth";

        @Bean
        public OpenAPI schoolManagementOpenApi() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("School Management Admin API")
                                                .description("Admin microservice APIs. Login at POST /api/v1/auth/login, then Authorize with Bearer JWT.")
                                                .version("v1")
                                                .contact(new Contact().name("School Management")))
                                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                                .components(new Components().addSecuritySchemes(
                                                BEARER,
                                                new SecurityScheme()
                                                                .name(BEARER)
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")));
        }
}
