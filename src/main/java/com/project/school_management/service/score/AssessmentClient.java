package com.project.school_management.service.score;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.project.school_management.config.AssessmentProperties;
import com.project.school_management.dto.AssessmentApiResponse;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.dto.dashboard.TopStudentRow;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.security.SchoolScopeService;

@Component
public class AssessmentClient {

    private final AssessmentProperties properties;
    private final SchoolScopeService schoolScopeService;

    public AssessmentClient(AssessmentProperties properties, SchoolScopeService schoolScopeService) {
        this.properties = properties;
        this.schoolScopeService = schoolScopeService;
    }

    private RestClient client() {
        String base = properties.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:8081";
        }
        return RestClient.builder().baseUrl(base.trim()).build();
    }

    public ScoreWireResponse create(Map<String, Object> body) {
        return post("/api/v1/scores", body, new ParameterizedTypeReference<AssessmentApiResponse<ScoreWireResponse>>() {
        });
    }

    public ScoreWireResponse update(UUID id, Map<String, Object> body) {
        return put("/api/v1/scores/" + id, body, new ParameterizedTypeReference<AssessmentApiResponse<ScoreWireResponse>>() {
        });
    }

    public int upsertBatch(Map<String, Object> body) {
        Map<String, Integer> data = post(
                "/api/v1/scores/batch",
                body,
                new ParameterizedTypeReference<AssessmentApiResponse<Map<String, Integer>>>() {
                });
        return data == null || data.get("saved") == null ? 0 : data.get("saved");
    }

    public ScoreWireResponse getById(UUID id) {
        return get("/api/v1/scores/" + id, new ParameterizedTypeReference<AssessmentApiResponse<ScoreWireResponse>>() {
        });
    }

    public List<ScoreWireResponse> list(UUID classUuid, Integer generation) {
        StringBuilder path = new StringBuilder("/api/v1/scores?");
        if (classUuid != null) {
            path.append("classUuid=").append(classUuid).append('&');
        }
        if (generation != null) {
            path.append("generation=").append(generation).append('&');
        }
        List<ScoreWireResponse> data = get(
                path.toString(),
                new ParameterizedTypeReference<AssessmentApiResponse<List<ScoreWireResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public List<ScoreWireResponse> listForSession(UUID classUuid, String subject, String term) {
        StringBuilder path = new StringBuilder("/api/v1/scores/session?classUuid=").append(classUuid);
        if (subject != null && !subject.isBlank()) {
            path.append("&subject=").append(encode(subject));
        }
        if (term != null && !term.isBlank()) {
            path.append("&term=").append(encode(term));
        }
        List<ScoreWireResponse> data = get(
                path.toString(),
                new ParameterizedTypeReference<AssessmentApiResponse<List<ScoreWireResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public List<ScoreWireResponse> listByStudent(UUID studentUuid, Integer generation, String term) {
        StringBuilder path = new StringBuilder("/api/v1/scores/students/").append(studentUuid).append('?');
        if (generation != null) {
            path.append("generation=").append(generation).append('&');
        }
        if (term != null && !term.isBlank()) {
            path.append("term=").append(encode(term)).append('&');
        }
        List<ScoreWireResponse> data = get(
                path.toString(),
                new ParameterizedTypeReference<AssessmentApiResponse<List<ScoreWireResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public StudentGpaResponse getStudentGpa(UUID studentUuid, Integer generation, String term) {
        StringBuilder path = new StringBuilder("/api/v1/scores/students/")
                .append(studentUuid).append("/gpa?");
        if (generation != null) {
            path.append("generation=").append(generation).append('&');
        }
        if (term != null && !term.isBlank()) {
            path.append("term=").append(encode(term)).append('&');
        }
        return get(path.toString(), new ParameterizedTypeReference<AssessmentApiResponse<StudentGpaResponse>>() {
        });
    }

    public void delete(UUID id) {
        exchangeDelete("/api/v1/scores/" + id);
    }

    public List<Integer> listGenerations(UUID studentUuid) {
        String path = studentUuid == null
                ? "/api/v1/scores/meta/generations"
                : "/api/v1/scores/meta/generations?studentUuid=" + studentUuid;
        List<Integer> data = get(path, new ParameterizedTypeReference<AssessmentApiResponse<List<Integer>>>() {
        });
        return data == null ? List.of() : data;
    }

    public List<String> listTerms(UUID studentUuid) {
        String path = studentUuid == null
                ? "/api/v1/scores/meta/terms"
                : "/api/v1/scores/meta/terms?studentUuid=" + studentUuid;
        List<String> data = get(path, new ParameterizedTypeReference<AssessmentApiResponse<List<String>>>() {
        });
        return data == null ? List.of() : data;
    }

    public List<UUID> listStudentUuids(Integer generation) {
        List<UUID> data = get(
                "/api/v1/scores/meta/student-uuids?generation=" + generation,
                new ParameterizedTypeReference<AssessmentApiResponse<List<UUID>>>() {
                });
        return data == null ? List.of() : data;
    }

    public GpaSummaryStats gpaSummary() {
        return get("/api/v1/dashboard/gpa-summary",
                new ParameterizedTypeReference<AssessmentApiResponse<GpaSummaryStats>>() {
                });
    }

    public List<TopStudentRow> topByClass() {
        List<TopStudentRow> data = get(
                "/api/v1/dashboard/top-by-class",
                new ParameterizedTypeReference<AssessmentApiResponse<List<TopStudentRow>>>() {
                });
        return data == null ? List.of() : data;
    }

    public List<TopStudentRow> topByGrade() {
        List<TopStudentRow> data = get(
                "/api/v1/dashboard/top-by-grade",
                new ParameterizedTypeReference<AssessmentApiResponse<List<TopStudentRow>>>() {
                });
        return data == null ? List.of() : data;
    }

    public ChartSeries termChart() {
        return get("/api/v1/dashboard/term-chart",
                new ParameterizedTypeReference<AssessmentApiResponse<ChartSeries>>() {
                });
    }

    public void setGpaAccess(UUID studentUuid, boolean approved, UUID approvedBy) {
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("approved", approved);
        payload.put("approvedByUuid", approvedBy);
        putInternal("/internal/v1/gpa-access/" + studentUuid, payload,
                new ParameterizedTypeReference<AssessmentApiResponse<Map<String, Object>>>() {
                });
    }

    public boolean hasGpaAccess(UUID studentUuid) {
        Map<String, Object> data = getInternal(
                "/internal/v1/gpa-access/" + studentUuid,
                new ParameterizedTypeReference<AssessmentApiResponse<Map<String, Object>>>() {
                });
        if (data == null || data.get("approved") == null) {
            return false;
        }
        return Boolean.TRUE.equals(data.get("approved"));
    }

    private <T> T get(String path, ParameterizedTypeReference<AssessmentApiResponse<T>> type) {
        try {
            AssessmentApiResponse<T> response = client().get()
                    .uri(path)
                    .headers(this::applyCallerHeaders)
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<AssessmentApiResponse<T>> type) {
        try {
            AssessmentApiResponse<T> response = client().post()
                    .uri(path)
                    .headers(this::applyCallerHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> T put(String path, Object body, ParameterizedTypeReference<AssessmentApiResponse<T>> type) {
        try {
            AssessmentApiResponse<T> response = client().put()
                    .uri(path)
                    .headers(this::applyCallerHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> T putInternal(String path, Object body, ParameterizedTypeReference<AssessmentApiResponse<T>> type) {
        try {
            AssessmentApiResponse<T> response = client().put()
                    .uri(path)
                    .header("X-Internal-Key", properties.getInternalKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> T getInternal(String path, ParameterizedTypeReference<AssessmentApiResponse<T>> type) {
        try {
            AssessmentApiResponse<T> response = client().get()
                    .uri(path)
                    .header("X-Internal-Key", properties.getInternalKey())
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private void exchangeDelete(String path) {
        try {
            client().delete()
                    .uri(path)
                    .headers(this::applyCallerHeaders)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assessment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private void applyCallerHeaders(HttpHeaders headers) {
        headers.set("X-Internal-Key", properties.getInternalKey());
        User user = schoolScopeService.requireCurrentUser();
        headers.set("X-User-Uuid", user.getUuid().toString());
        headers.set("X-User-Email", user.getEmail());
        RoleName role = user.getRole() != null ? user.getRole().getName() : null;
        if (role != null) {
            headers.set("X-User-Role", role.name());
        }
        if (user.getSchool() != null && user.getSchool().getUuid() != null
                && role != RoleName.ADMIN && role != RoleName.SUPERADMIN) {
            headers.set("X-School-Uuid", user.getSchool().getUuid().toString());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            headers.set("X-Authorities", authorities);
        }
    }

    private static <T> T unwrap(AssessmentApiResponse<T> response) {
        if (response == null) {
            throw new IllegalStateException("Empty response from assessment service");
        }
        if (!response.isSuccess()) {
            throw new IllegalArgumentException(
                    response.getMessage() == null ? "Assessment service error" : response.getMessage());
        }
        return response.getData();
    }

    private static RuntimeException translate(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String body = ex.getResponseBodyAsString();
        String message = body == null || body.isBlank() ? ex.getMessage() : body;
        if (status.value() == 404) {
            return new ExceptionNotFound(message);
        }
        if (status.value() == 403) {
            return new org.springframework.security.access.AccessDeniedException(message);
        }
        if (status.value() == 400) {
            return new IllegalArgumentException(extractMessage(message));
        }
        return new IllegalStateException("Assessment service error (" + status.value() + "): " + extractMessage(message),
                ex);
    }

    private static String extractMessage(String raw) {
        if (raw == null) {
            return "Unknown error";
        }
        // Spring default JSON: {"status":400,"detail":"..."} or {"message":"..."}
        int detail = raw.indexOf("\"detail\":\"");
        if (detail >= 0) {
            int start = detail + 10;
            int end = raw.indexOf('"', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        int msg = raw.indexOf("\"message\":\"");
        if (msg >= 0) {
            int start = msg + 11;
            int end = raw.indexOf('"', start);
            if (end > start) {
                return raw.substring(start, end);
            }
        }
        return raw.length() > 300 ? raw.substring(0, 300) : raw;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Wire DTO matching assessment ScoreResponse JSON. */
    @lombok.Getter
    @lombok.Setter
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreWireResponse {
        private UUID uuid;
        private UUID studentUuid;
        private String studentName;
        private UUID classUuid;
        private String className;
        private Integer generation;
        private String generationDisplay;
        private String subject;
        private String term;
        private java.math.BigDecimal score;
        private java.math.BigDecimal maxScore;
        private String remark;
        private UUID teacherUuid;
        private String teacherName;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

        public com.project.school_management.dto.score.ScoreResponse toDto() {
            return com.project.school_management.dto.score.ScoreResponse.builder()
                    .uuid(uuid)
                    .studentUuid(studentUuid)
                    .studentName(studentName)
                    .classUuid(classUuid)
                    .className(className)
                    .generation(generation)
                    .generationDisplay(generationDisplay)
                    .subject(subject)
                    .term(term)
                    .score(score)
                    .maxScore(maxScore)
                    .remark(remark)
                    .teacherUuid(teacherUuid)
                    .teacherName(teacherName)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }
    }

    public static List<com.project.school_management.dto.score.ScoreResponse> toDtos(List<ScoreWireResponse> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(ScoreWireResponse::toDto).toList();
    }
}
