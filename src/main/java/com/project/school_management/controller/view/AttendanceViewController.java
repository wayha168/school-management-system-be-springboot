package com.project.school_management.controller.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceMarkItem;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.attendance.ClassAttendanceOverview;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserClassItem;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.enums.AttendanceStatus;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.attendance.AttendanceService;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.user.UserService;

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
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String view,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "attendance", "Attendance");
            DataUser account = (DataUser) model.getAttribute("account");
            boolean management = isManagement(account);
            // Prefer tab= for client switch; keep view= as alias
            String activeTab = normalizeView(tab != null ? tab : view, management, account);

            UUID userFilter = account != null && account.getRole() == RoleName.STUDENT ? account.getUuid() : null;
            RoleName roleFilter = parseRole(role);

            List<AttendanceResponse> records = attendanceService.list(date, classUuid, userFilter);
            if (roleFilter != null) {
                records = records.stream()
                        .filter(r -> r.getUserRole() == roleFilter)
                        .toList();
            }

            List<SchoolClassResponse> classes = visibleClasses(account);
            model.addAttribute("records", records);
            model.addAttribute("classes", classes);
            model.addAttribute("classOverviews", buildClassOverviews(classes));
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedClassUuid", classUuid);
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
            List<SchoolClassResponse> allowed = visibleClasses(account);
            SchoolClassResponse schoolClass = allowed.stream()
                    .filter(c -> classUuid.equals(c.getUuid()))
                    .findFirst()
                    .orElse(null);
            if (schoolClass == null) {
                ra.addFlashAttribute("error", "Class not found or not allowed");
                return "redirect:/admin/attendance";
            }

            LocalDate day = date != null ? date : LocalDate.now();
            List<UserResponse> students = studentsInClass(classUuid);
            List<UserResponse> teachers = teachersForClass(classUuid);
            Map<UUID, AttendanceResponse> byUser = attendanceService.list(day, classUuid, null).stream()
                    .filter(r -> r.getUserUuid() != null)
                    .collect(Collectors.toMap(AttendanceResponse::getUserUuid, r -> r, (a, b) -> a, LinkedHashMap::new));

            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("teachers", teachers);
            model.addAttribute("students", students);
            model.addAttribute("attendanceByUser", byUser);
            model.addAttribute("selectedDate", day);
            model.addAttribute("statuses", AttendanceStatus.values());
            model.addAttribute("canMark", canMark(account));
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
        List<SchoolClassResponse> classes = markableClasses(account);
        model.addAttribute("classes", classes);
        model.addAttribute("selectedClassUuid", classUuid);
        model.addAttribute("selectedDate", day);
        model.addAttribute("statuses", AttendanceStatus.values());
        List<UserResponse> students = List.of();
        if (classUuid != null) {
            boolean allowed = classes.stream().anyMatch(c -> classUuid.equals(c.getUuid()));
            if (allowed) {
                students = studentsInClass(classUuid);
            }
        }
        model.addAttribute("students", students);
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
            AttendanceBatchRequest batch = new AttendanceBatchRequest();
            batch.setClassUuid(classUuid);
            batch.setAttendanceDate(attendanceDate);
            List<AttendanceMarkItem> items = new ArrayList<>();
            for (int i = 0; i < studentUuid.size(); i++) {
                AttendanceMarkItem item = new AttendanceMarkItem();
                item.setUserUuid(studentUuid.get(i));
                item.setStatus(i < status.size() ? status.get(i) : AttendanceStatus.PRESENT);
                items.add(item);
            }
            batch.setItems(items);
            int count = attendanceService.markBatch(batch);
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

    private List<ClassAttendanceOverview> buildClassOverviews(List<SchoolClassResponse> classes) {
        List<UserResponse> allUsers = userService.getAll();
        List<ClassAttendanceOverview> rows = new ArrayList<>();
        for (SchoolClassResponse c : classes) {
            List<String> teacherNames = allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.TEACHER)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .map(UserResponse::getName)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            int studentCount = (int) allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.STUDENT)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .count();
            String teachersLabel = teacherNames.isEmpty() ? "Unassigned" : String.join(", ", teacherNames);
            rows.add(ClassAttendanceOverview.builder()
                    .classUuid(c.getUuid())
                    .className(c.getName())
                    .grade(c.getGrade())
                    .generation(c.getGeneration())
                    .generationCode(c.getGenerationCode())
                    .schoolName(c.getSchoolName())
                    .teacherNames(teacherNames)
                    .teachersLabel(teachersLabel)
                    .studentCount(studentCount)
                    .build());
        }
        rows.sort(Comparator.comparing(ClassAttendanceOverview::getClassName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private List<UserResponse> studentsInClass(UUID classUuid) {
        return userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.STUDENT)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(UserResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<UserResponse> teachersForClass(UUID classUuid) {
        return userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.TEACHER)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(UserResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<SchoolClassResponse> visibleClasses(DataUser account) {
        List<SchoolClassResponse> all = schoolClassService.getAll();
        if (account == null || account.getRole() == null) {
            return List.of();
        }
        if (isManagement(account) || account.getRole() == RoleName.PRINCIPAL || account.getRole() == RoleName.STAFF) {
            return all;
        }
        if (account.getRole() == RoleName.TEACHER) {
            return markableClasses(account);
        }
        if (account.getRole() == RoleName.STUDENT && account.getClasses() != null) {
            Set<UUID> mine = account.getClasses().stream().map(UserClassItem::getUuid).collect(Collectors.toSet());
            return all.stream().filter(c -> mine.contains(c.getUuid())).toList();
        }
        return List.of();
    }

    private List<SchoolClassResponse> markableClasses(DataUser account) {
        List<SchoolClassResponse> all = schoolClassService.getAll();
        if (account == null || account.getRole() == null) {
            return List.of();
        }
        if (account.getRole() == RoleName.SUPERADMIN || account.getRole() == RoleName.ADMIN) {
            return all;
        }
        if (account.getRole() == RoleName.TEACHER) {
            if (account.getClasses() == null || account.getClasses().isEmpty()) {
                return List.of();
            }
            Set<UUID> taught = account.getClasses().stream().map(UserClassItem::getUuid).collect(Collectors.toSet());
            return all.stream().filter(c -> taught.contains(c.getUuid())).toList();
        }
        return List.of();
    }

    private static boolean isManagement(DataUser account) {
        return account != null
                && (account.getRole() == RoleName.SUPERADMIN || account.getRole() == RoleName.ADMIN);
    }

    private static boolean canMark(DataUser account) {
        if (account == null || account.getRole() == null) {
            return false;
        }
        return account.getRole() == RoleName.SUPERADMIN
                || account.getRole() == RoleName.ADMIN
                || account.getRole() == RoleName.TEACHER;
    }

    private static String normalizeView(String view, boolean management, DataUser account) {
        if ("records".equalsIgnoreCase(view)) {
            return "records";
        }
        if ("classes".equalsIgnoreCase(view)) {
            return "classes";
        }
        if (management || (account != null && account.getRole() == RoleName.TEACHER)) {
            return "classes";
        }
        return "records";
    }

    private static RoleName parseRole(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return null;
        }
        try {
            return RoleName.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
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
