package com.project.assessment.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GpaAccessResponse {

    private UUID studentUuid;
    private boolean approved;
}
