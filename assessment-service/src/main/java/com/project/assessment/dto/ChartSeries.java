package com.project.assessment.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartSeries {

    private List<String> labels;
    private List<Number> values;
}
