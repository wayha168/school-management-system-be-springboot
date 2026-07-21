package com.project.assessment.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.assessment.dto.ApiResponse;
import com.project.assessment.dto.ScoreBatchUpsertRequest;
import com.project.assessment.dto.ScoreResponse;
import com.project.assessment.dto.ScoreUpsertRequest;
import com.project.assessment.dto.StudentGpaResponse;
import com.project.assessment.security.CallerContext;
import com.project.assessment.security.CallerResolver;
import com.project.assessment.service.ScoreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/scores")
public class ScoreController {

    private final ScoreService scoreService;
    private final CallerResolver callerResolver;

    public ScoreController(ScoreService scoreService, CallerResolver callerResolver) {
        this.scoreService = scoreService;
        this.callerResolver = callerResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScoreResponse>> create(@Valid @RequestBody ScoreUpsertRequest request) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Score created", scoreService.create(request, caller)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> batch(
            @Valid @RequestBody ScoreBatchUpsertRequest request) {
        CallerContext caller = callerResolver.require();
        int saved = scoreService.upsertBatch(request, caller);
        return ResponseEntity.ok(ApiResponse.ok("Scores saved", Map.of("saved", saved)));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> session(
            @RequestParam UUID classUuid,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String term) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok(
                "Session scores fetched",
                scoreService.listForSession(classUuid, subject, term, caller)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> myScores(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        CallerContext caller = callerResolver.require();
        if (caller.userUuid() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("User uuid required"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "My scores fetched",
                scoreService.listByStudent(caller.userUuid(), generation, term, caller)));
    }

    @GetMapping("/me/gpa")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> myGpa(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        CallerContext caller = callerResolver.require();
        if (caller.userUuid() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("User uuid required"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "My GPA fetched",
                scoreService.getStudentGpa(caller.userUuid(), generation, term, caller)));
    }

    @GetMapping("/students/{studentUuid}")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> studentScores(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok(
                "Student scores fetched",
                scoreService.listByStudent(studentUuid, generation, term, caller)));
    }

    @GetMapping("/students/{studentUuid}/gpa")
    public ResponseEntity<ApiResponse<StudentGpaResponse>> studentGpa(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok(
                "Student GPA fetched",
                scoreService.getStudentGpa(studentUuid, generation, term, caller)));
    }

    @GetMapping("/meta/generations")
    public ResponseEntity<ApiResponse<List<Integer>>> generations(
            @RequestParam(required = false) UUID studentUuid) {
        callerResolver.require();
        List<Integer> gens = studentUuid == null
                ? scoreService.listScoreGenerations()
                : scoreService.listScoreGenerationsForStudent(studentUuid);
        return ResponseEntity.ok(ApiResponse.ok("Generations fetched", gens));
    }

    @GetMapping("/meta/terms")
    public ResponseEntity<ApiResponse<List<String>>> terms(
            @RequestParam(required = false) UUID studentUuid) {
        callerResolver.require();
        List<String> terms = studentUuid == null
                ? scoreService.listScoreTerms()
                : scoreService.listScoreTermsForStudent(studentUuid);
        return ResponseEntity.ok(ApiResponse.ok("Terms fetched", terms));
    }

    @GetMapping("/meta/student-uuids")
    public ResponseEntity<ApiResponse<List<UUID>>> studentUuids(@RequestParam Integer generation) {
        callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok(
                "Student uuids fetched",
                scoreService.listStudentUuidsWithScores(generation)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> list(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) Integer generation) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok("Scores fetched", scoreService.list(classUuid, generation, caller)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScoreResponse>> get(@PathVariable UUID id) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok("Score fetched", scoreService.getById(id, caller)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScoreResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScoreUpsertRequest request) {
        CallerContext caller = callerResolver.require();
        return ResponseEntity.ok(ApiResponse.ok("Score updated", scoreService.update(id, request, caller)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        CallerContext caller = callerResolver.require();
        scoreService.delete(id, caller);
        return ResponseEntity.ok(ApiResponse.ok("Score deleted", null));
    }
}
