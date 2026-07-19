package com.project.school_management.dto.dashboard;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartSeries {

    private List<String> labels;
    private List<Number> values;
    private List<Number> present;
    private List<Number> absent;
    private List<Number> late;
}
