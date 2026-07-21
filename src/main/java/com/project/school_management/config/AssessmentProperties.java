package com.project.school_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.assessment")
public class AssessmentProperties {

    private String baseUrl = "http://localhost:8081";
    private String internalKey = "assessment-internal-key-change-me";
}
