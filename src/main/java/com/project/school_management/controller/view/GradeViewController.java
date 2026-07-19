package com.project.school_management.controller.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
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

import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.score.ClassStudentGradeRow;
import com.project.school_management.dto.score.StudentGpaResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserClassItem;
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
            @RequestParam(required = false) UUID classUuid,
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

            List<Integer> sessions = gradeSessions();
            List<SchoolClassResponse> classes = generation == null
                    ? schoolClassService.getAll()
                    : schoolClassService.getByGeneration(generation);

            // Keep selected class only if it still appears for this session
            final UUID selectedClass = classUuid != null
                    && classes.stream().anyMatch(c -> classUuid.equals(c.getUuid()))
                            ? classUuid
                            : null;

            List<ClassStudentGradeRow> studentRows = List.of();
            SchoolClassResponse selectedClassInfo = null;
            BigDecimal classAvgGpa = null;
            BigDecimal classAvgPercent = null;
            if (selectedClass != null) {
                selectedClassInfo = classes.stream()
                        .filter(c -> selectedClass.equals(c.getUuid()))
                        .findFirst()
                        .orElse(null);
                studentRows = studentsInClass(selectedClass, generation);
                if (!studentRows.isEmpty()) {
                    BigDecimal gpaSum = BigDecimal.ZERO;
                    BigDecimal pctSum = BigDecimal.ZERO;
                    int counted = 0;
                    for (ClassStudentGradeRow row : studentRows) {
                        if (row.getTotalScores() > 0) {
                            gpaSum = gpaSum.add(row.getGpa());
                            pctSum = pctSum.add(row.getAveragePercent());
                            counted++;
                        }
                    }
                    if (counted > 0) {
                        classAvgGpa = gpaSum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
                        classAvgPercent = pctSum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
                    }
                }
            }

            model.addAttribute("classes", classes);
            model.addAttribute("students", studentRows);
            model.addAttribute("generations", sessions);
            model.addAttribute("terms", scoreService.listScoreTerms());
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedClassUuid", selectedClass);
            model.addAttribute("selectedClass", selectedClassInfo);
            model.addAttribute("classAvgGpa", classAvgGpa);
            model.addAttribute("classAvgPercent", classAvgPercent);
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
            @RequestParam(required = false) UUID classUuid,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "grades", "Student grades");
            DataUser account = (DataUser) model.getAttribute("account");
            if (account != null && account.getRole() == RoleName.STUDENT
                    && !account.getUuid().equals(studentUuid)) {
                ra.addFlashAttribute("error", "You can only view your own grades");
                return "redirect:/admin/grades/" + account.getUuid();
            }

            StudentGpaResponse gpa = scoreService.getStudentGpa(studentUuid, generation, term);
            model.addAttribute("gpa", gpa);
            model.addAttribute("generations", studentGradeSessions(studentUuid));
            model.addAttribute("terms", scoreService.listScoreTermsForStudent(studentUuid));
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedTerm", term);
            model.addAttribute("selectedClassUuid", classUuid);
            model.addAttribute("isStudentSelf",
                    account != null && account.getRole() == RoleName.STUDENT);

            if (classUuid != null && (account == null || account.getRole() != RoleName.STUDENT)) {
                model.addAttribute("classStudents", studentsInClass(classUuid, generation));
            } else {
                model.addAttribute("classStudents", List.of());
            }
            return "pages/grade-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/grades";
        }
    }

    private List<ClassStudentGradeRow> studentsInClass(UUID classUuid, Integer generation) {
        List<UserResponse> students = userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.STUDENT)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<ClassStudentGradeRow> rows = new ArrayList<>();
        for (UserResponse student : students) {
            StudentGpaResponse gpa = scoreService.getStudentGpa(student.getUuid(), generation, null);
            rows.add(ClassStudentGradeRow.builder()
                    .studentUuid(student.getUuid())
                    .studentName(student.getName())
                    .studentEmail(student.getEmail())
                    .grade(student.getGrade())
                    .gpa(gpa.getGpa())
                    .averagePercent(gpa.getAveragePercent())
                    .letterGrade(gpa.getLetterGrade())
                    .totalScores(gpa.getTotalScores())
                    .build());
        }
        return rows;
    }

    /** Sessions = class generations + score generations. */
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
                    .map(UserClassItem::getGeneration)
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
