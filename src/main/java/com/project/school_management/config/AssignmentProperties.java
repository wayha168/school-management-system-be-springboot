package com.project.school_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.assignment")
public class AssignmentProperties {

    private String baseUrl = "http://localhost:8082";
    private String internalKey = "assignment-internal-key-change-me";
}
