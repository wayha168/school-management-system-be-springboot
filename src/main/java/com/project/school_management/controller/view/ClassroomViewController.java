package com.project.school_management.controller.view;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @RequestParam String meetingUrl,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledAt,
            RedirectAttributes ra) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("classUuid", classUuid);
            body.put("title", title);
            body.put("meetingUrl", meetingUrl);
            if (provider != null && !provider.isBlank()) {
                body.put("provider", provider.trim());
            }
            if (scheduledAt != null) {
                body.put("scheduledAt", scheduledAt);
            }
            assignmentClient.createMeeting(body);
            ra.addFlashAttribute("success", "Class meeting created");
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
            @RequestParam String content,
            RedirectAttributes ra) {
        try {
            assignmentClient.submit(id, Map.of("content", content));
            ra.addFlashAttribute("success", "Submission saved");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes/" + classUuid;
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
