package com.project.school_management.service.classroom;

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

import com.project.school_management.config.AssignmentProperties;
import com.project.school_management.dto.AssignmentApiResponse;
import com.project.school_management.dto.classroom.ClassroomDashboard;
import com.project.school_management.dto.classroom.HomeworkAssignmentResponse;
import com.project.school_management.dto.classroom.MeetingResponse;
import com.project.school_management.dto.classroom.SubmissionResponse;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.security.SchoolScopeService;

@Component
public class AssignmentClient {

    private final AssignmentProperties properties;
    private final SchoolScopeService schoolScopeService;

    public AssignmentClient(AssignmentProperties properties, SchoolScopeService schoolScopeService) {
        this.properties = properties;
        this.schoolScopeService = schoolScopeService;
    }

    private RestClient client() {
        String base = properties.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:8082";
        }
        return RestClient.builder().baseUrl(base.trim()).build();
    }

    public MeetingResponse createMeeting(Map<String, Object> body) {
        return post("/api/v1/meetings", body,
                new ParameterizedTypeReference<AssignmentApiResponse<MeetingResponse>>() {
                });
    }

    public List<MeetingResponse> listMeetings(UUID classUuid) {
        List<MeetingResponse> data = get(
                "/api/v1/meetings?classUuid=" + classUuid,
                new ParameterizedTypeReference<AssignmentApiResponse<List<MeetingResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public MeetingResponse endMeeting(UUID id) {
        return post("/api/v1/meetings/" + id + "/end", Map.of(),
                new ParameterizedTypeReference<AssignmentApiResponse<MeetingResponse>>() {
                });
    }

    public HomeworkAssignmentResponse createAssignment(Map<String, Object> body) {
        return post("/api/v1/assignments", body,
                new ParameterizedTypeReference<AssignmentApiResponse<HomeworkAssignmentResponse>>() {
                });
    }

    public List<HomeworkAssignmentResponse> listAssignments(UUID classUuid) {
        List<HomeworkAssignmentResponse> data = get(
                "/api/v1/assignments?classUuid=" + classUuid,
                new ParameterizedTypeReference<AssignmentApiResponse<List<HomeworkAssignmentResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public HomeworkAssignmentResponse closeAssignment(UUID id) {
        return post("/api/v1/assignments/" + id + "/close", Map.of(),
                new ParameterizedTypeReference<AssignmentApiResponse<HomeworkAssignmentResponse>>() {
                });
    }

    public SubmissionResponse submit(UUID assignmentId, Map<String, Object> body) {
        return post("/api/v1/assignments/" + assignmentId + "/submit", body,
                new ParameterizedTypeReference<AssignmentApiResponse<SubmissionResponse>>() {
                });
    }

    public List<SubmissionResponse> listSubmissions(UUID assignmentId) {
        List<SubmissionResponse> data = get(
                "/api/v1/assignments/" + assignmentId + "/submissions",
                new ParameterizedTypeReference<AssignmentApiResponse<List<SubmissionResponse>>>() {
                });
        return data == null ? List.of() : data;
    }

    public ClassroomDashboard dashboard(List<UUID> classUuids) {
        StringBuilder path = new StringBuilder("/api/v1/classroom/dashboard?");
        if (classUuids != null) {
            for (UUID id : classUuids) {
                if (id != null) {
                    path.append("classUuid=").append(id).append('&');
                }
            }
        }
        ClassroomDashboard data = get(
                path.toString(),
                new ParameterizedTypeReference<AssignmentApiResponse<ClassroomDashboard>>() {
                });
        if (data == null) {
            ClassroomDashboard empty = new ClassroomDashboard();
            empty.setActiveMeetings(Collections.emptyList());
            empty.setOpenAssignments(Collections.emptyList());
            return empty;
        }
        if (data.getActiveMeetings() == null) {
            data.setActiveMeetings(Collections.emptyList());
        }
        if (data.getOpenAssignments() == null) {
            data.setOpenAssignments(Collections.emptyList());
        }
        return data;
    }

    private <T> T get(String path, ParameterizedTypeReference<AssignmentApiResponse<T>> type) {
        try {
            AssignmentApiResponse<T> response = client().get()
                    .uri(path)
                    .headers(this::applyCallerHeaders)
                    .retrieve()
                    .body(type);
            return unwrap(response);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Cannot reach assignment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
        }
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<AssignmentApiResponse<T>> type) {
        try {
            AssignmentApiResponse<T> response = client().post()
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
                    "Cannot reach assignment service at " + properties.getBaseUrl() + ": " + ex.getMessage(), ex);
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

    private static <T> T unwrap(AssignmentApiResponse<T> response) {
        if (response == null) {
            throw new IllegalStateException("Empty response from assignment service");
        }
        if (!response.isSuccess()) {
            throw new IllegalStateException(
                    response.getMessage() != null ? response.getMessage() : "Assignment service error");
        }
        return response.getData();
    }

    private static RuntimeException translate(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = extractMessage(ex.getResponseBodyAsString());
        if (status.value() == 404) {
            return new ExceptionNotFound(message);
        }
        if (status.value() == 403) {
            return new org.springframework.security.access.AccessDeniedException(message);
        }
        if (status.value() == 400) {
            return new IllegalArgumentException(message);
        }
        return new IllegalStateException(
                "Assignment service error (" + status.value() + "): " + message, ex);
    }

    private static String extractMessage(String raw) {
        if (raw == null) {
            return "Unknown error";
        }
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
}
