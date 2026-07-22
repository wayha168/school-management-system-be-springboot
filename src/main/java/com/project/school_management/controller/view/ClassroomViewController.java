package com.project.school_management.controller.view;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.school_management.dto.classroom.MeetingResponse;
import com.project.school_management.dto.classroom.SubmissionResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.classroom.AssignmentClient;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.user.UserService;

@Controller
@RequestMapping("/admin/classroom")
public class ClassroomViewController {

    private final AssignmentClient assignmentClient;
    private final SchoolClassService schoolClassService;
    private final UserService userService;
    private final PresenceTracker presenceTracker;

    public ClassroomViewController(
            AssignmentClient assignmentClient,
            SchoolClassService schoolClassService,
            UserService userService,
            PresenceTracker presenceTracker) {
        this.assignmentClient = assignmentClient;
        this.schoolClassService = schoolClassService;
        this.userService = userService;
        this.presenceTracker = presenceTracker;
    }

    @PostMapping("/meetings")
    @PreAuthorize("hasAuthority('MEETING_WRITE')")
    public String createMeeting(
            @RequestParam UUID classUuid,
            @RequestParam String title,
            @RequestParam(defaultValue = "false") boolean recordEnabled,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledAt,
            RedirectAttributes ra) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("classUuid", classUuid);
            body.put("title", title);
            body.put("recordEnabled", recordEnabled);
            if (scheduledAt != null) {
                body.put("scheduledAt", scheduledAt);
            }
            MeetingResponse created = assignmentClient.createMeeting(body);
            String join = created.resolveJoinPath();
            ra.addFlashAttribute("success",
                    "Video meeting started. Join link: " + join + " (room " + created.getRoomCode() + ")");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
    }

    @PostMapping("/meetings/{id}/end")
    @PreAuthorize("hasAuthority('MEETING_WRITE')")
    public String endMeeting(
            @PathVariable UUID id,
            @RequestParam UUID classUuid,
            RedirectAttributes ra) {
        try {
            assignmentClient.endMeeting(id);
            ra.addFlashAttribute("success", "Meeting ended");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
    }

    @GetMapping("/call/{roomCode}")
    @PreAuthorize("hasAuthority('MEETING_READ')")
    public String joinCall(
            @PathVariable String roomCode,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            MeetingResponse meeting = assignmentClient.getMeetingByRoom(roomCode);
            fillCommon(model, authentication, "classes", meeting.getTitle());
            DataUser account = (DataUser) model.getAttribute("account");
            model.addAttribute("meeting", meeting);
            model.addAttribute("roomCode", meeting.getRoomCode());
            model.addAttribute("peerId", account != null ? account.getUuid().toString() : UUID.randomUUID().toString());
            model.addAttribute("displayName", account != null ? account.getName() : "Guest");
            model.addAttribute("canRecord",
                    meeting.isRecordEnabled() && authentication.getAuthorities().stream()
                            .anyMatch(a -> "MEETING_WRITE".equals(a.getAuthority())));
            model.addAttribute("canManage",
                    authentication.getAuthorities().stream()
                            .anyMatch(a -> "MEETING_WRITE".equals(a.getAuthority())));
            return "pages/video-call";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/meetings/{id}/recording")
    @PreAuthorize("hasAuthority('MEETING_WRITE')")
    @ResponseBody
    public Map<String, Object> uploadRecording(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        MeetingResponse saved = assignmentClient.uploadRecording(id, file);
        return Map.of(
                "success", true,
                "message", "Recording stored",
                "hasRecording", saved.isHasRecording(),
                "recordingBytes", saved.getRecordingBytes() != null ? saved.getRecordingBytes() : 0L);
    }

    @GetMapping("/meetings/{id}/recording")
    @PreAuthorize("hasAuthority('MEETING_READ')")
    public ResponseEntity<byte[]> downloadRecording(@PathVariable UUID id) {
        MeetingResponse meta = assignmentClient.getMeeting(id);
        byte[] bytes = assignmentClient.downloadRecording(id);
        String contentType = meta.getRecordingContentType() != null ? meta.getRecordingContentType() : "video/webm";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getTitle() + "-recording.webm\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('ASSIGNMENT_WRITE')")
    public String createAssignment(
            @RequestParam UUID classUuid,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dueAt,
            RedirectAttributes ra) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("classUuid", classUuid);
            body.put("title", title);
            if (description != null && !description.isBlank()) {
                body.put("description", description.trim());
            }
            if (dueAt != null) {
                body.put("dueAt", dueAt);
            }
            assignmentClient.createAssignment(body);
            ra.addFlashAttribute("success", "Assignment created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
    }

    @PostMapping("/assignments/{id}/close")
    @PreAuthorize("hasAuthority('ASSIGNMENT_WRITE')")
    public String closeAssignment(
            @PathVariable UUID id,
            @RequestParam UUID classUuid,
            RedirectAttributes ra) {
        try {
            assignmentClient.closeAssignment(id);
            ra.addFlashAttribute("success", "Assignment closed");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
    }

    @PostMapping("/assignments/{id}/submit")
    @PreAuthorize("hasAnyAuthority('ASSIGNMENT_WRITE','ASSIGNMENT_READ')")
    public String submitAssignment(
            @PathVariable UUID id,
            @RequestParam UUID classUuid,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) MultipartFile file,
            RedirectAttributes ra) {
        try {
            boolean hasText = content != null && !content.isBlank();
            boolean hasFile = file != null && !file.isEmpty();
            if (!hasText && !hasFile) {
                throw new IllegalArgumentException("Add text or upload a file/image");
            }
            if (hasFile) {
                assignmentClient.submitWithFile(id, content, file);
            } else {
                assignmentClient.submit(id, Map.of("content", content.trim()));
            }
            ra.addFlashAttribute("success", "Submission saved");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
    }

    @GetMapping("/assignments/{assignmentId}/submissions/{submissionId}/attachment")
    @PreAuthorize("hasAnyAuthority('ASSIGNMENT_WRITE','ASSIGNMENT_READ')")
    public ResponseEntity<byte[]> downloadSubmissionAttachment(
            @PathVariable UUID assignmentId,
            @PathVariable UUID submissionId) {
        return assignmentClient.downloadSubmissionAttachment(assignmentId, submissionId);
    }

    @GetMapping("/assignments/{id}/submissions")
    @PreAuthorize("hasAuthority('ASSIGNMENT_WRITE')")
    public String listSubmissions(
            @PathVariable UUID id,
            @RequestParam UUID classUuid,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Submissions");
            model.addAttribute("schoolClass", schoolClassService.getById(classUuid));
            model.addAttribute("assignmentId", id);
            List<SubmissionResponse> submissions = assignmentClient.listSubmissions(id);
            model.addAttribute("submissions", submissions);
            return "pages/assignment-submissions";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes/" + classUuid;
        }
    }

    private void fillCommon(Model model, Authentication authentication, String activePage, String pageTitle) {
        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFound("No authenticated account");
        }
        DataUser account = userService.getAccountByEmail(authentication.getName());
        model.addAttribute("account", account);
        model.addAttribute("dataUser", account);
        model.addAttribute("username", account.getEmail());
        model.addAttribute("activePage", activePage);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("onlineCount", presenceTracker.snapshot().getOnlineCount());
    }
}
