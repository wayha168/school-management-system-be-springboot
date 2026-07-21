package com.project.assessment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.assessment.dto.ApiResponse;
import com.project.assessment.dto.ChartSeries;
import com.project.assessment.dto.GpaSummaryStats;
import com.project.assessment.dto.TopStudentRow;
import com.project.assessment.security.CallerResolver;
import com.project.assessment.service.ScoreService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final ScoreService scoreService;
    private final CallerResolver callerResolver;

    public DashboardController(ScoreService scoreService, CallerResolver callerResolver) {
        this.scoreService = scoreService;
        this.callerResolver = callerResolver;
    }

    @GetMapping("/gpa-summary")
    public ResponseEntity<ApiResponse<GpaSummaryStats>> gpaSummary() {
        return ResponseEntity.ok(ApiResponse.ok(
                "GPA summary fetched",
                scoreService.gpaSummary(callerResolver.require())));
    }

    @GetMapping("/top-by-class")
    public ResponseEntity<ApiResponse<List<TopStudentRow>>> topByClass() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top by class fetched",
                scoreService.topStudentsByClass(callerResolver.require())));
    }

    @GetMapping("/top-by-grade")
    public ResponseEntity<ApiResponse<List<TopStudentRow>>> topByGrade() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top by grade fetched",
                scoreService.topStudentsByGrade(callerResolver.require())));
    }

    @GetMapping("/term-chart")
    public ResponseEntity<ApiResponse<ChartSeries>> termChart() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Term chart fetched",
                scoreService.termScoreChart(callerResolver.require())));
    }
}
