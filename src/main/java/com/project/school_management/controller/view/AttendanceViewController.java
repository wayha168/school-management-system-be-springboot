package com.project.school_management.controller.view;

import java.time.LocalDate;
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

import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.enums.AttendanceStatus;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.attendance.AttendanceService;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.user.UserService;

/**
 * Thin attendance view controller — HTTP/model only.
 * Business logic is in {@link AttendanceService} / {@link com.project.school_management.service.attendance.AttendanceServiceImpl}.
 */
@Controller
@RequestMapping("/admin/attendance")
public class AttendanceViewController {

    private static final List<RoleName> ATTENDANCE_ROLE_TABS = List.of(
            RoleName.STUDENT,
            RoleName.TEACHER,
            RoleName.STAFF,
            RoleName.PRINCIPAL,
            RoleName.ADMIN,
            RoleName.SUPERADMIN);

    private final AttendanceService attendanceService;
    private final SchoolClassService schoolClassService;
    private final UserService userService;
    private final PresenceTracker presenceTracker;

    public AttendanceViewController(
            AttendanceService attendanceService,
            SchoolClassService schoolClassService,
            UserService userService,
            PresenceTracker presenceTracker) {
        this.attendanceService = attendanceService;
        this.schoolClassService = schoolClassService;
        this.userService = userService;
        this.presenceTracker = presenceTracker;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public String list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String view,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "attendance", "Attendance");
            DataUser account = (DataUser) model.getAttribute("account");
            boolean management = attendanceService.isManagement(account);
            String activeTab = attendanceService.resolveActiveTab(tab, view, account);
            UUID userFilter = account != null && account.getRole() == RoleName.STUDENT ? account.getUuid() : null;
            RoleName roleFilter = attendanceService.parseRole(role);

            List<AttendanceResponse> records = attendanceService.listFiltered(
                    date, classUuid, userFilter, roleFilter, generation);
            List<SchoolClassResponse> filteredClasses = attendanceService.visibleClasses(account, generation);

            model.addAttribute("records", records);
            model.addAttribute("classes", filteredClasses);
            model.addAttribute("classOverviews", attendanceService.buildClassOverviews(filteredClasses));
            model.addAttribute("generations", schoolClassService.listGenerations());
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedClassUuid", classUuid);
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedRole", roleFilter);
            model.addAttribute("roleTabs", ATTENDANCE_ROLE_TABS);
            model.addAttribute("activeTab", activeTab);
            model.addAttribute("managementView", management);
            model.addAttribute("statuses", AttendanceStatus.values());
            return "pages/attendance";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load attendance");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/classes/{classUuid}")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    public String classDetail(
            @PathVariable UUID classUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "attendance", "Class attendance");
            DataUser account = (DataUser) model.getAttribute("account");
            SchoolClassResponse schoolClass = attendanceService.findVisibleClass(account, classUuid);
            if (schoolClass == null) {
                ra.addFlashAttribute("error", "Class not found or not allowed");
                return "redirect:/admin/attendance";
            }

            LocalDate day = date != null ? date : LocalDate.now();
            var monthChart = attendanceService.classMonthChart(classUuid, 30);
            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("teachers", attendanceService.teachersForClass(classUuid));
            model.addAttribute("students", attendanceService.studentsInClass(classUuid));
            model.addAttribute("attendanceByUser", attendanceService.attendanceByUser(day, classUuid));
            model.addAttribute("selectedDate", day);
            model.addAttribute("statuses", AttendanceStatus.values());
            model.addAttribute("canMark", attendanceService.canMark(account));
            model.addAttribute("classMonthLabels", monthChart.getLabels());
            model.addAttribute("classMonthPresent", monthChart.getPresent());
            model.addAttribute("classMonthAbsent", monthChart.getAbsent());
            model.addAttribute("classMonthLate", monthChart.getLate());
            return "pages/attendance-class";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/attendance";
        }
    }

    @GetMapping("/mark")
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    public String markForm(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication,
            Model model) {
        fillCommon(model, authentication, "attendance", "Mark attendance");
        DataUser account = (DataUser) model.getAttribute("account");
        LocalDate day = date != null ? date : LocalDate.now();
        List<SchoolClassResponse> classes = attendanceService.markableClasses(account);
        model.addAttribute("classes", classes);
        model.addAttribute("selectedClassUuid", classUuid);
        model.addAttribute("selectedDate", day);
        model.addAttribute("statuses", AttendanceStatus.values());

        List<UserResponse> students = List.of();
        Map<UUID, AttendanceResponse> byUser = Map.of();
        if (classUuid != null && classes.stream().anyMatch(c -> classUuid.equals(c.getUuid()))) {
            students = attendanceService.studentsInClass(classUuid);
            byUser = attendanceService.attendanceByUser(day, classUuid);
        }
        model.addAttribute("students", students);
        model.addAttribute("attendanceByUser", byUser);
        return "pages/attendance-mark";
    }

    @PostMapping("/mark")
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    public String markSubmit(
            @RequestParam UUID classUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestParam List<UUID> studentUuid,
            @RequestParam List<AttendanceStatus> status,
            RedirectAttributes ra) {
        try {
            int count = attendanceService.markFromForm(classUuid, attendanceDate, studentUuid, status);
            ra.addFlashAttribute("success", "Marked " + count + " attendance row(s)");
            return "redirect:/admin/attendance/classes/" + classUuid + "?date=" + attendanceDate;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/attendance/mark?classUuid=" + classUuid + "&date=" + attendanceDate;
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_WRITE')")
    public String upsertSingle(
            @RequestParam UUID userUuid,
            @RequestParam(required = false) UUID classUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestParam AttendanceStatus status,
            @RequestParam(required = false) String remark,
            RedirectAttributes ra) {
        try {
            AttendanceRequest request = new AttendanceRequest();
            request.setUserUuid(userUuid);
            request.setClassUuid(classUuid);
            request.setAttendanceDate(attendanceDate);
            request.setStatus(status);
            request.setRemark(remark);
            attendanceService.upsert(request);
            ra.addFlashAttribute("success", "Attendance saved");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        if (classUuid != null) {
            return "redirect:/admin/attendance/classes/" + classUuid + "?date=" + attendanceDate;
        }
        return "redirect:/admin/attendance?tab=records";
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
