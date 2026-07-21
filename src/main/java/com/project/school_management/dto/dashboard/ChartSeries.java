package com.project.school_management.dto.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartSeries {

    private List<String> labels;
    private List<Number> values;
    private List<Number> present;
    private List<Number> absent;
    private List<Number> late;
}
