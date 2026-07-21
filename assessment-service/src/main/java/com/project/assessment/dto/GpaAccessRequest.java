package com.project.assessment.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GpaAccessRequest {

    private boolean approved;
    private UUID approvedByUuid;
}
