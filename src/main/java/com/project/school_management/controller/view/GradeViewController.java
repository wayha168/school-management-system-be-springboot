package com.project.school_management.controller.view;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.score.ScoreService;
import com.project.school_management.service.user.UserService;

@Controller
@RequestMapping("/admin/grades")
public class GradeViewController {

    private final ScoreService scoreService;
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final PresenceTracker presenceTracker;

    public GradeViewController(
            ScoreService scoreService,
            UserService userService,
            SchoolClassService schoolClassService,
            PresenceTracker presenceTracker) {
        this.scoreService = scoreService;
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.presenceTracker = presenceTracker;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCORE_READ')")
    public String list(
            @RequestParam(required = false) Integer generation,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "grades", "Grades");
            DataUser account = (DataUser) model.getAttribute("account");
            if (account != null && account.getRole() == RoleName.STUDENT) {
                return "redirect:/admin/grades/" + account.getUuid()
                        + (generation != null ? "?generation=" + generation : "");
            }
            Set<UUID> scoredInGeneration = generation == null
                    ? Set.of()
                    : new LinkedHashSet<>(scoreService.listStudentUuidsWithScores(generation));
            List<UserResponse> students = userService.getAll().stream()
                    .filter(u -> u.getRole() == RoleName.STUDENT)
                    .filter(u -> generation == null
                            || scoredInGeneration.contains(u.getUuid())
                            || (u.getClasses() != null && u.getClasses().stream()
                                    .anyMatch(c -> generation.equals(c.getGeneration()))))
                    .toList();
            model.addAttribute("students", students);
            model.addAttribute("generations", gradeSessions());
            model.addAttribute("terms", scoreService.listScoreTerms());
            model.addAttribute("selectedGeneration", generation);
            return "pages/grades";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load grades");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/{studentUuid}")
    @PreAuthorize("hasAuthority('SCORE_READ')")
    public String detail(
            @PathVariable UUID studentUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String term,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "grades", "Student grades");
            StudentGpaResponse gpa = scoreService.getStudentGpa(studentUuid, generation, term);
            model.addAttribute("gpa", gpa);
            model.addAttribute("generations", studentGradeSessions(studentUuid));
            model.addAttribute("terms", scoreService.listScoreTermsForStudent(studentUuid));
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedTerm", term);
            return "pages/grade-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/grades";
        }
    }

    /** All generations from classes + scores that need grade display. */
    private List<Integer> gradeSessions() {
        Set<Integer> gens = new LinkedHashSet<>(schoolClassService.listGenerations());
        gens.addAll(scoreService.listScoreGenerations());
        return gens.stream().sorted().toList();
    }

    private List<Integer> studentGradeSessions(UUID studentUuid) {
        Set<Integer> gens = new LinkedHashSet<>(scoreService.listScoreGenerationsForStudent(studentUuid));
        UserResponse student = userService.getById(studentUuid);
        if (student.getClasses() != null) {
            student.getClasses().stream()
                    .map(c -> c.getGeneration())
                    .filter(g -> g != null)
                    .forEach(gens::add);
        }
        return gens.stream().sorted().toList();
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
