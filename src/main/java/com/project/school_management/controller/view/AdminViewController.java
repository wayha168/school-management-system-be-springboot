package com.project.school_management.controller.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.project.school_management.dto.permission.PermissionAssignRequest;
import com.project.school_management.dto.permission.RolePermissionRequest;
import com.project.school_management.dto.role.RoleRequest;
import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;
import com.project.school_management.enums.Permission;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.dashboard.GpaSummaryStats;
import com.project.school_management.service.attendance.AttendanceService;
import com.project.school_management.service.permission.PermissionService;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.role.RoleService;
import com.project.school_management.service.school.SchoolService;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.score.ScoreService;
import com.project.school_management.service.user.UserService;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    private final SchoolService schoolService;
    private final UserService userService;
    private final SchoolClassService schoolClassService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final PresenceTracker presenceTracker;
    private final SchoolScopeService schoolScopeService;
    private final ScoreService scoreService;
    private final AttendanceService attendanceService;

    public AdminViewController(
            SchoolService schoolService,
            UserService userService,
            SchoolClassService schoolClassService,
            RoleService roleService,
            PermissionService permissionService,
            UserRepository userRepository,
            PresenceTracker presenceTracker,
            SchoolScopeService schoolScopeService,
            ScoreService scoreService,
            AttendanceService attendanceService) {
        this.schoolService = schoolService;
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.presenceTracker = presenceTracker;
        this.schoolScopeService = schoolScopeService;
        this.scoreService = scoreService;
        this.attendanceService = attendanceService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            fillCommon(model, authentication, "dashboard", "Dashboard");
            model.addAttribute("schoolCount", schoolService.getAll().size());
            model.addAttribute("userCount", userService.getAll().size());
            model.addAttribute("classCount", schoolClassService.getAll().size());
            model.addAttribute("roleCount", roleService.getAll().size());
            var schoolScope = schoolScopeService.scopedSchoolUuid();
            if (schoolScope.isPresent()) {
                UUID schoolUuid = schoolScope.get();
                model.addAttribute("teacherCount", userRepository.countBySchool_UuidAndRole_Name(schoolUuid, RoleName.TEACHER));
                model.addAttribute("studentCount", userRepository.countBySchool_UuidAndRole_Name(schoolUuid, RoleName.STUDENT));
                model.addAttribute("staffCount", userRepository.countBySchool_UuidAndRole_Name(schoolUuid, RoleName.STAFF));
            } else {
                model.addAttribute("teacherCount", userRepository.countByRole_Name(RoleName.TEACHER));
                model.addAttribute("studentCount", userRepository.countByRole_Name(RoleName.STUDENT));
                model.addAttribute("staffCount", userRepository.countByRole_Name(RoleName.STAFF));
            }
            model.addAttribute("permissionCount", Permission.values().length);
            model.addAttribute("onlineCount", presenceTracker.snapshot().getOnlineCount());
            model.addAttribute(
                    "chartLabels",
                    List.of("Users", "Teachers", "Students", "Staff", "Classes", "Schools"));
            model.addAttribute(
                    "chartValues",
                    List.of(
                            model.getAttribute("userCount"),
                            model.getAttribute("teacherCount"),
                            model.getAttribute("studentCount"),
                            model.getAttribute("staffCount"),
                            model.getAttribute("classCount"),
                            model.getAttribute("schoolCount")));

            try {
                GpaSummaryStats gpaSummary = scoreService.gpaSummary();
                ChartSeries termChart = scoreService.termScoreChart();
                model.addAttribute("gpaSummary", gpaSummary);
                model.addAttribute("topByClass", scoreService.topStudentsByClass());
                model.addAttribute("topByGrade", scoreService.topStudentsByGrade());
                model.addAttribute("termChartLabels", termChart.getLabels() != null ? termChart.getLabels() : List.of());
                model.addAttribute("termChartValues", termChart.getValues() != null ? termChart.getValues() : List.of());
            } catch (Exception ex) {
                // Assessment service down/misconfigured must not block login → dashboard
                model.addAttribute("gpaSummary", GpaSummaryStats.builder()
                        .averageGpa(java.math.BigDecimal.ZERO)
                        .averagePercent(java.math.BigDecimal.ZERO)
                        .studentsWithScores(0)
                        .totalScoreRows(0)
                        .topLetter("—")
                        .build());
                model.addAttribute("topByClass", List.of());
                model.addAttribute("topByGrade", List.of());
                model.addAttribute("termChartLabels", List.of());
                model.addAttribute("termChartValues", List.of());
                model.addAttribute("assessmentWarning", "Score analytics temporarily unavailable");
            }
            ChartSeries attendanceMonth = attendanceService.lastMonthChart(30);
            model.addAttribute("attendanceMonthLabels", attendanceMonth.getLabels());
            model.addAttribute("attendancePresent", attendanceMonth.getPresent());
            model.addAttribute("attendanceAbsent", attendanceMonth.getAbsent());
            model.addAttribute("attendanceLate", attendanceMonth.getLate());

            DataUser account = (DataUser) model.getAttribute("account");
            boolean canAssignAttendance = account != null
                    && account.getPermissions() != null
                    && account.getPermissions().contains("ATTENDANCE_WRITE");
            model.addAttribute("canAssignAttendance", canAssignAttendance);
            return "pages/dashboard";
        } catch (UserNotFound | ErrorRuntime ex) {
            // Do not redirect to /login?error — Spring shows that as bad credentials
            model.addAttribute("error", "Unable to load dashboard: " + ex.getMessage());
            model.addAttribute("page", "dashboard");
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("account", null);
            return "pages/dashboard";
        } catch (Exception ex) {
            // Never send users back to login?error — that looks like bad credentials
            redirectAttributes.addFlashAttribute("error",
                    "Unable to load dashboard: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
            try {
                fillCommon(model, authentication, "dashboard", "Dashboard");
            } catch (Exception ignored) {
                model.addAttribute("error", "Dashboard error after login. Check server logs.");
                model.addAttribute("page", "dashboard");
                model.addAttribute("pageTitle", "Dashboard");
                model.addAttribute("account", null);
                return "pages/dashboard";
            }
            model.addAttribute("schoolCount", 0);
            model.addAttribute("userCount", 0);
            model.addAttribute("classCount", 0);
            model.addAttribute("roleCount", 0);
            model.addAttribute("teacherCount", 0);
            model.addAttribute("studentCount", 0);
            model.addAttribute("staffCount", 0);
            model.addAttribute("permissionCount", 0);
            model.addAttribute("onlineCount", 0);
            model.addAttribute("chartLabels", List.of());
            model.addAttribute("chartValues", List.of());
            model.addAttribute("gpaSummary", GpaSummaryStats.builder()
                    .averageGpa(java.math.BigDecimal.ZERO)
                    .averagePercent(java.math.BigDecimal.ZERO)
                    .studentsWithScores(0)
                    .totalScoreRows(0)
                    .topLetter("—")
                    .build());
            model.addAttribute("topByClass", List.of());
            model.addAttribute("topByGrade", List.of());
            model.addAttribute("termChartLabels", List.of());
            model.addAttribute("termChartValues", List.of());
            model.addAttribute("attendanceMonthLabels", List.of());
            model.addAttribute("attendancePresent", List.of());
            model.addAttribute("attendanceAbsent", List.of());
            model.addAttribute("attendanceLate", List.of());
            model.addAttribute("canAssignAttendance", false);
            return "pages/dashboard";
        }
    }

    // ── Schools ──────────────────────────────────────────────────────────────

    @GetMapping("/schools")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    public String schools(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "schools", "Schools");
            model.addAttribute("schools", schoolService.getAll());
            return "pages/schools";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load schools");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/schools/new")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String schoolCreateForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "schools", "Add School");
        model.addAttribute("mode", "create");
        return "pages/school-form";
    }

    @GetMapping("/schools/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    public String schoolView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "schools", "School");
            model.addAttribute("school", schoolService.getById(id));
            model.addAttribute("mode", "view");
            return "pages/school-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools";
        }
    }

    @GetMapping("/schools/{id}/edit")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String schoolEditForm(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "schools", "Edit School");
            model.addAttribute("school", schoolService.getById(id));
            model.addAttribute("mode", "edit");
            return "pages/school-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools";
        }
    }

    @PostMapping(path = "/schools", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String createSchool(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String address,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String website,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile banner,
            RedirectAttributes ra) {
        try {
            SchoolRequest request = buildSchoolRequest(name, description, address, phone, email, website);
            schoolService.create(request, logo, banner);
            ra.addFlashAttribute("success", "School created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools/new";
        }
        return "redirect:/admin/schools";
    }

    @PostMapping(path = "/schools/{id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String updateSchool(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String address,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String website,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile banner,
            RedirectAttributes ra) {
        try {
            SchoolRequest request = buildSchoolRequest(name, description, address, phone, email, website);
            schoolService.update(id, request, logo, banner);
            ra.addFlashAttribute("success", "School updated");
            return "redirect:/admin/schools/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools/" + id + "/edit";
        }
    }

    @GetMapping("/schools/{id}/logo")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @ResponseBody
    public ResponseEntity<byte[]> schoolLogo(@PathVariable UUID id) {
        SchoolImage image = schoolService.getLogo(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    @GetMapping("/schools/{id}/banner")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @ResponseBody
    public ResponseEntity<byte[]> schoolBanner(@PathVariable UUID id) {
        SchoolImage image = schoolService.getBanner(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    @PostMapping("/schools/{id}/delete")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String deleteSchool(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            schoolService.delete(id);
            ra.addFlashAttribute("success", "School deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/schools";
    }

    // ── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public String users(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) RoleName role,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "users", "Users");
            List<UserResponse> users = userService.getAll();
            if (role != null) {
                users = users.stream()
                        .filter(u -> u.getRole() == role)
                        .toList();
            }
            if (generation != null) {
                users = users.stream()
                        .filter(u -> u.getClasses() != null
                                && u.getClasses().stream().anyMatch(c -> generation.equals(c.getGeneration())))
                        .toList();
            }
            model.addAttribute("users", users);
            model.addAttribute("generations", schoolClassService.listGenerations());
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedRole", role);
            model.addAttribute("roleFilters", List.of(
                    RoleName.TEACHER, RoleName.STUDENT, RoleName.STAFF,
                    RoleName.PRINCIPAL, RoleName.ADMIN, RoleName.SUPERADMIN));
            model.addAttribute("onlineEmails", presenceTracker.snapshot().getOnlineEmails());
            return "pages/users";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load users");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String userCreateForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "users", "Add User");
        fillUserFormOptions(model);
        model.addAttribute("mode", "create");
        return "pages/user-form";
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public String userView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "users", "User");
            UserResponse user = userService.getById(id);
            model.addAttribute("user", user);
            model.addAttribute("online", presenceTracker.isOnline(user.getEmail()));
            return "pages/user-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/users/{id}/edit")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String userEditForm(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "users", "Edit User");
            model.addAttribute("user", userService.getById(id));
            fillUserFormOptions(model);
            model.addAttribute("mode", "edit");
            return "pages/user-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping(path = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String createUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UUID roleUuid,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) List<UUID> classUuids,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) BigDecimal salary,
            @RequestParam(required = false) MultipartFile profileImage,
            RedirectAttributes ra) {
        try {
            UserRequest request = new UserRequest();
            request.setName(UserResponse.joinName(firstName, lastName));
            request.setEmail(email);
            request.setPassword(password);
            request.setRoleUuid(roleUuid);
            request.setSchoolUuid(schoolUuid);
            request.setClassUuids(classUuids);
            request.setGrade(grade);
            request.setRoom(room);
            request.setSalary(salary);
            userService.create(request, profileImage);
            ra.addFlashAttribute("success", "User created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    @PostMapping(path = "/users/{id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String updateUser(
            @PathVariable UUID id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam UUID roleUuid,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) List<UUID> classUuids,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) BigDecimal salary,
            @RequestParam(required = false) MultipartFile profileImage,
            RedirectAttributes ra) {
        try {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setName(UserResponse.joinName(firstName, lastName));
            request.setEmail(email);
            request.setPassword(blankToNull(password));
            request.setRoleUuid(roleUuid);
            request.setSchoolUuid(schoolUuid);
            request.setClassUuids(classUuids);
            request.setGrade(grade);
            request.setRoom(room);
            request.setSalary(salary);
            userService.update(id, request, profileImage);
            ra.addFlashAttribute("success", "User updated");
            return "redirect:/admin/users/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/delete")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            userService.delete(id);
            ra.addFlashAttribute("success", "User deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/avatar")
    @PreAuthorize("hasAuthority('USER_READ')")
    @ResponseBody
    public ResponseEntity<byte[]> userAvatar(@PathVariable UUID id) {
        SchoolImage image = userService.getProfileImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    // ── Classes ──────────────────────────────────────────────────────────────

    @GetMapping("/classes")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    public String classes(
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String grade,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Classes");
            model.addAttribute("classes", schoolClassService.filter(generation, grade));
            model.addAttribute("generations", schoolClassService.listGenerations());
            model.addAttribute("grades", schoolClassService.listGrades());
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("selectedGrade", grade);
            return "pages/classes";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load classes");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/classes/new")
    @PreAuthorize("hasAuthority('CLASS_WRITE')")
    public String classCreateForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "classes", "Add Class");
        model.addAttribute("schools", schoolService.getAll());
        model.addAttribute("teachers", schoolClassService.listAssignableTeachers());
        model.addAttribute("mode", "create");
        return "pages/class-form";
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    public String classView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Class");
            model.addAttribute("item", schoolClassService.getById(id));
            model.addAttribute("teachers", schoolClassService.teachersForClass(id));
            return "pages/class-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @GetMapping("/classes/{id}/edit")
    @PreAuthorize("hasAuthority('CLASS_WRITE')")
    public String classEditForm(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Edit Class");
            model.addAttribute("item", schoolClassService.getById(id));
            model.addAttribute("schools", schoolService.getAll());
            model.addAttribute("teachers", schoolClassService.listAssignableTeachers());
            model.addAttribute("mode", "edit");
            return "pages/class-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/classes")
    @PreAuthorize("hasAuthority('CLASS_WRITE')")
    public String createClass(
            @RequestParam String name,
            @RequestParam(required = false) String grade,
            @RequestParam Integer generation,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) List<String> subjects,
            @RequestParam(required = false) List<UUID> teacherUuids,
            RedirectAttributes ra) {
        try {
            SchoolClassRequest request = new SchoolClassRequest();
            request.setName(name);
            request.setGrade(blankToNull(grade));
            request.setGeneration(generation);
            request.setAcademicYear(academicYear);
            request.setSchoolUuid(schoolUuid);
            request.setSubjects(subjects);
            request.setTeacherUuids(teacherUuids != null ? teacherUuids : List.of());
            schoolClassService.create(request);
            ra.addFlashAttribute("success", "Class created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes/new";
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/classes/{id}/update")
    @PreAuthorize("hasAuthority('CLASS_WRITE')")
    public String updateClass(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String grade,
            @RequestParam Integer generation,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) List<String> subjects,
            @RequestParam(required = false) List<UUID> teacherUuids,
            RedirectAttributes ra) {
        try {
            SchoolClassRequest request = new SchoolClassRequest();
            request.setName(name);
            request.setGrade(blankToNull(grade));
            request.setGeneration(generation);
            request.setAcademicYear(academicYear);
            request.setSchoolUuid(schoolUuid);
            request.setSubjects(subjects);
            request.setTeacherUuids(teacherUuids != null ? teacherUuids : List.of());
            schoolClassService.update(id, request);
            ra.addFlashAttribute("success", "Class updated");
            return "redirect:/admin/classes/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/classes/" + id + "/edit";
        }
    }

    @PostMapping("/classes/{id}/delete")
    @PreAuthorize("hasAuthority('CLASS_WRITE')")
    public String deleteClass(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            schoolClassService.delete(id);
            ra.addFlashAttribute("success", "Class deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/classes";
    }

    // ── Scores ───────────────────────────────────────────────────────────────

    @GetMapping("/scores")
    @PreAuthorize("hasAuthority('SCORE_READ')")
    public String scores(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) Integer generation,
            @RequestParam(required = false) String tab,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "scores", "Scores");
            DataUser account = (DataUser) model.getAttribute("account");
            if (account != null && account.getRole() == RoleName.STUDENT) {
                return "redirect:/admin/grades/" + account.getUuid()
                        + (generation != null ? "?generation=" + generation : "");
            }

            List<SchoolClassResponse> classes = scoreService.visibleClassesForScoring(account, generation);
            List<Integer> generations = classes.stream()
                    .map(SchoolClassResponse::getGeneration)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
            String activeTab = "records".equalsIgnoreCase(tab) ? "records" : "classes";

            List<ScoreResponse> scores = List.of();
            if ("records".equals(activeTab)) {
                try {
                    scores = scoreService.list(classUuid, generation);
                } catch (Exception ex) {
                    model.addAttribute("assessmentWarning", assessmentUnavailableMessage(ex));
                }
            }

            model.addAttribute("classOverviews", scoreService.buildClassOverviews(classes));
            model.addAttribute("scores", scores);
            model.addAttribute("classes", classes);
            model.addAttribute("generations", generations);
            model.addAttribute("selectedClassUuid", classUuid);
            model.addAttribute("selectedGeneration", generation);
            model.addAttribute("activeTab", activeTab);
            model.addAttribute("canEnterScores", scoreService.canEnterScores(account));
            return "pages/scores";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load scores: " + safeMessage(ex));
            return "redirect:/admin/dashboard";
        }
    }

    /** Attendance-style score entry: Class + Subject/Period, then student table with score inputs. */
    @GetMapping("/scores/mark")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreMarkForm(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "scores", "Input scores");
            DataUser account = (DataUser) model.getAttribute("account");
            List<SchoolClassResponse> allowed = scoreService.visibleClassesForScoring(account, null);
            model.addAttribute("classes", allowed);

            String selectedPeriod = scoreService.normalizePeriodKey(period);
            String selectedTerm = scoreService.resolveTermFromPeriod(selectedPeriod);
            String monthlyLabel = scoreService.currentMonthlyLabel();
            BigDecimal selectedMax = maxScore != null ? maxScore : BigDecimal.valueOf(100);

            model.addAttribute("selectedPeriod", selectedPeriod);
            model.addAttribute("selectedTerm", selectedTerm);
            model.addAttribute("monthlyLabel", monthlyLabel);
            model.addAttribute("selectedMaxScore", selectedMax);

            if (classUuid == null) {
                model.addAttribute("selectedClassUuid", null);
                model.addAttribute("schoolClass", null);
                model.addAttribute("classSubjects", List.of());
                model.addAttribute("students", List.of());
                model.addAttribute("scoreByStudent", Map.of());
                model.addAttribute("selectedSubject", null);
                model.addAttribute("hasSubjects", false);
                model.addAttribute("ready", false);
                return "pages/score-class";
            }

            SchoolClassResponse schoolClass = allowed.stream()
                    .filter(c -> classUuid.equals(c.getUuid()))
                    .findFirst()
                    .orElse(null);
            if (schoolClass == null) {
                ra.addFlashAttribute("error", "Class not found or not allowed");
                return "redirect:/admin/scores";
            }

            List<String> classSubjects = schoolClass.getSubjects() != null ? schoolClass.getSubjects() : List.of();
            String selectedSubject = scoreService.resolveSubject(classSubjects, subject);
            List<UserResponse> students = scoreService.studentsInClass(classUuid);
            Map<UUID, ScoreResponse> scoreByStudent = Map.of();
            if (selectedSubject != null) {
                try {
                    scoreByStudent = scoreService.scoresByStudentForSession(
                            classUuid, selectedSubject, selectedTerm);
                    if (!scoreByStudent.isEmpty() && maxScore == null) {
                        ScoreResponse sample = scoreByStudent.values().iterator().next();
                        if (sample.getMaxScore() != null) {
                            selectedMax = sample.getMaxScore();
                        }
                    }
                } catch (Exception ex) {
                    model.addAttribute("assessmentWarning", assessmentUnavailableMessage(ex));
                }
            }

            model.addAttribute("schoolClass", schoolClass);
            model.addAttribute("selectedClassUuid", classUuid);
            model.addAttribute("selectedGeneration", schoolClass.getGeneration());
            model.addAttribute("selectedSubject", selectedSubject);
            model.addAttribute("selectedMaxScore", selectedMax);
            model.addAttribute("classSubjects", classSubjects);
            model.addAttribute("students", students);
            model.addAttribute("studentCount", students.size());
            model.addAttribute("scoreByStudent", scoreByStudent);
            model.addAttribute("hasSubjects", !classSubjects.isEmpty());
            model.addAttribute("ready", selectedSubject != null && !students.isEmpty());
            return "pages/score-class";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", assessmentUnavailableMessage(ex));
            return "redirect:/admin/scores";
        }
    }

    @GetMapping("/scores/classes/{classUuid}")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreClassForm(
            @PathVariable UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore) {
        return "redirect:" + scoreMarkRedirect(classUuid, period, subject, maxScore);
    }

    @PostMapping("/scores/mark")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreMarkSubmit(
            @RequestParam UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) List<UUID> studentUuid,
            @RequestParam(required = false) List<String> entryScore,
            RedirectAttributes ra) {
        String selectedPeriod = scoreService.normalizePeriodKey(period);
        String selectedTerm = scoreService.resolveTermFromPeriod(selectedPeriod);
        String redirect = scoreMarkRedirect(classUuid, selectedPeriod, subject, maxScore);
        try {
            int count = scoreService.upsertClassSession(
                    classUuid, selectedPeriod, subject, maxScore, studentUuid, entryScore);
            ra.addFlashAttribute("success",
                    "Saved " + count + " score(s) · " + selectedTerm
                            + (subject != null ? " · " + subject.trim() : ""));
            return "redirect:" + redirect;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + redirect;
        }
    }

    @PostMapping("/scores/classes/{classUuid}")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreClassSubmit(
            @PathVariable UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) List<UUID> studentUuid,
            @RequestParam(required = false) List<String> entryScore,
            RedirectAttributes ra) {
        return scoreMarkSubmit(classUuid, period, subject, maxScore, studentUuid, entryScore, ra);
    }

    /** Legacy entry — redirect into attendance-style score mark page. */
    @GetMapping("/scores/session")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreSessionForm(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore) {
        if (classUuid == null) {
            return "redirect:/admin/scores/mark";
        }
        String periodKey = period != null && !period.isBlank()
                ? scoreService.normalizePeriodKey(period)
                : scoreService.normalizePeriodKey(term);
        return "redirect:" + scoreMarkRedirect(classUuid, periodKey, subject, maxScore);
    }

    @PostMapping("/scores/session")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreSessionSubmit(
            @RequestParam UUID classUuid,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) List<UUID> studentUuid,
            @RequestParam(required = false) List<String> entryScore,
            RedirectAttributes ra) {
        return scoreMarkSubmit(classUuid, period, subject, maxScore, studentUuid, entryScore, ra);
    }

    @GetMapping("/scores/new")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreCreateForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "scores", "Add Score");
        fillScoreFormOptions(model);
        model.addAttribute("mode", "create");
        return "pages/score-form";
    }

    @GetMapping("/scores/import")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreImportForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "scores", "Import Scores");
        model.addAttribute("classes", schoolClassService.getAll());
        return "pages/score-import";
    }

    @GetMapping("/scores/export")
    @PreAuthorize("hasAuthority('SCORE_READ')")
    public ResponseEntity<byte[]> exportScores(
            @RequestParam(required = false) UUID classUuid,
            @RequestParam(required = false) Integer generation) {
        byte[] bytes = scoreService.exportExcel(classUuid, generation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=scores.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/scores/{id}")
    @PreAuthorize("hasAuthority('SCORE_READ')")
    public String scoreView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "scores", "Score");
            model.addAttribute("item", scoreService.getById(id));
            return "pages/score-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/scores";
        }
    }

    @GetMapping("/scores/{id}/edit")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String scoreEditForm(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "scores", "Edit Score");
            fillScoreFormOptions(model);
            model.addAttribute("item", scoreService.getById(id));
            model.addAttribute("mode", "edit");
            return "pages/score-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/scores";
        }
    }

    @PostMapping("/scores")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String createScore(
            @RequestParam UUID studentUuid,
            @RequestParam UUID classUuid,
            @RequestParam String subject,
            @RequestParam(required = false) String term,
            @RequestParam BigDecimal score,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) String remark,
            RedirectAttributes ra) {
        try {
            scoreService.create(buildScoreRequest(studentUuid, classUuid, subject, scoreService.resolveTermFromForm(term), score, maxScore, remark));
            ra.addFlashAttribute("success", "Score created");
            return "redirect:/admin/scores?tab=records&classUuid=" + classUuid;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/scores/new";
        }
    }

    @PostMapping("/scores/{id}/update")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String updateScore(
            @PathVariable UUID id,
            @RequestParam UUID studentUuid,
            @RequestParam UUID classUuid,
            @RequestParam String subject,
            @RequestParam(required = false) String term,
            @RequestParam BigDecimal score,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) String remark,
            RedirectAttributes ra) {
        try {
            scoreService.update(id, buildScoreRequest(studentUuid, classUuid, subject, scoreService.resolveTermFromForm(term), score, maxScore, remark));
            ra.addFlashAttribute("success", "Score updated");
            return "redirect:/admin/scores/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/scores/" + id + "/edit";
        }
    }

    @PostMapping("/scores/{id}/delete")
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String deleteScore(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            scoreService.delete(id);
            ra.addFlashAttribute("success", "Score deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/scores";
    }

    @PostMapping(path = "/scores/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCORE_WRITE')")
    public String importScores(
            @RequestParam UUID classUuid,
            @RequestParam MultipartFile file,
            RedirectAttributes ra) {
        try {
            int count = scoreService.importExcel(file, classUuid);
            ra.addFlashAttribute("success", "Imported " + count + " score row(s)");
            return "redirect:/admin/scores?classUuid=" + classUuid;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/scores/import";
        }
    }

    private void fillScoreFormOptions(Model model) {
        model.addAttribute("classes", schoolClassService.getAll());
        model.addAttribute("students", userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.STUDENT)
                .toList());
    }

    private String scoreMarkRedirect(UUID classUuid, String period, String subject, BigDecimal maxScore) {
        if (classUuid == null) {
            return "/admin/scores/mark";
        }
        String periodKey = scoreService.normalizePeriodKey(period);
        return "/admin/scores/mark?classUuid=" + classUuid
                + "&period=" + urlEncode(periodKey)
                + (subject != null && !subject.isBlank() ? "&subject=" + urlEncode(subject) : "")
                + (maxScore != null ? "&maxScore=" + maxScore : "");
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static ScoreRequest buildScoreRequest(
            UUID studentUuid,
            UUID classUuid,
            String subject,
            String term,
            BigDecimal score,
            BigDecimal maxScore,
            String remark) {
        ScoreRequest request = new ScoreRequest();
        request.setStudentUuid(studentUuid);
        request.setClassUuid(classUuid);
        request.setSubject(subject);
        request.setTerm(term);
        request.setScore(score);
        request.setMaxScore(maxScore);
        request.setRemark(remark);
        return request;
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    public String roles(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "roles", "Roles");
            model.addAttribute("roles", roleService.getAll());
            return "pages/roles";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load roles");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/roles/new")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String roleCreateForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "roles", "Add Role");
        model.addAttribute("roleNames", RoleName.values());
        model.addAttribute("mode", "create");
        return "pages/role-form";
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    public String roleView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "roles", "Role");
            model.addAttribute("role", roleService.getById(id));
            return "pages/role-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/roles";
        }
    }

    @GetMapping("/roles/{id}/edit")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String roleEditForm(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "roles", "Edit Role");
            model.addAttribute("role", roleService.getById(id));
            model.addAttribute("roleNames", RoleName.values());
            model.addAttribute("mode", "edit");
            return "pages/role-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/roles";
        }
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String createRole(
            @RequestParam RoleName name,
            @RequestParam String description,
            RedirectAttributes ra) {
        try {
            RoleRequest request = new RoleRequest();
            request.setName(name);
            request.setDescription(description);
            roleService.create(request);
            ra.addFlashAttribute("success", "Role created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/roles/new";
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/{id}/update")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String updateRole(
            @PathVariable UUID id,
            @RequestParam RoleName name,
            @RequestParam String description,
            RedirectAttributes ra) {
        try {
            RoleRequest request = new RoleRequest();
            request.setName(name);
            request.setDescription(description);
            roleService.update(id, request);
            ra.addFlashAttribute("success", "Role updated");
            return "redirect:/admin/roles/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/roles/" + id + "/edit";
        }
    }

    @PostMapping("/roles/{id}/delete")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String deleteRole(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            roleService.delete(id);
            ra.addFlashAttribute("success", "Role deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    public String permissions(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "permissions", "Permissions");
            model.addAttribute("permissions", permissionService.getAll());
            return "pages/permissions";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load permissions");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/permissions/assign")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String permissionAssignForm(
            @RequestParam(required = false) UUID roleUuid,
            Authentication authentication,
            Model model) {
        fillCommon(model, authentication, "permissions", "Assign Permission");
        fillPermissionFormOptions(model, roleUuid);
        return "pages/permission-assign";
    }

    @GetMapping("/permissions/revoke")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String permissionRevokeForm(
            @RequestParam(required = false) UUID roleUuid,
            Authentication authentication,
            Model model) {
        fillCommon(model, authentication, "permissions", "Revoke Permission");
        fillPermissionFormOptions(model, roleUuid);
        return "pages/permission-revoke";
    }

    @GetMapping("/permissions/replace")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String permissionReplaceForm(
            @RequestParam(required = false) UUID roleUuid,
            Authentication authentication,
            Model model) {
        fillCommon(model, authentication, "permissions", "Replace Permissions");
        fillPermissionFormOptions(model, roleUuid);
        return "pages/permission-replace";
    }

    @PostMapping("/permissions/assign")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String assignPermission(
            @RequestParam UUID roleUuid,
            @RequestParam(required = false) List<String> permissions,
            RedirectAttributes ra) {
        try {
            if (permissions == null || permissions.isEmpty()) {
                throw new ErrorRuntime("Select at least one permission");
            }
            int count = 0;
            for (String permission : permissions) {
                PermissionAssignRequest request = new PermissionAssignRequest();
                request.setRoleUuid(roleUuid);
                request.setPermission(permission);
                permissionService.assign(request);
                count++;
            }
            ra.addFlashAttribute("success", "Assigned " + count + " permission(s)");
            return "redirect:/admin/permissions";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/permissions/assign?roleUuid=" + roleUuid;
        }
    }

    @PostMapping("/permissions/revoke")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String revokePermission(
            @RequestParam UUID roleUuid,
            @RequestParam(required = false) List<String> permissions,
            RedirectAttributes ra) {
        try {
            if (permissions == null || permissions.isEmpty()) {
                throw new ErrorRuntime("Select at least one permission to revoke");
            }
            for (String permission : permissions) {
                permissionService.revoke(roleUuid, permission);
            }
            ra.addFlashAttribute("success", "Revoked " + permissions.size() + " permission(s)");
            return "redirect:/admin/permissions";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/permissions/revoke?roleUuid=" + roleUuid;
        }
    }

    @PostMapping("/permissions/replace")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String replacePermissions(
            @RequestParam UUID roleUuid,
            @RequestParam(required = false) List<String> permissions,
            RedirectAttributes ra) {
        try {
            RolePermissionRequest request = new RolePermissionRequest();
            request.setRoleUuid(roleUuid);
            request.setPermissions(permissions != null ? permissions : List.of());
            if (request.getPermissions().isEmpty()) {
                throw new ErrorRuntime("Select at least one permission");
            }
            permissionService.replaceRolePermissions(request);
            ra.addFlashAttribute("success", "Role permissions updated");
            return "redirect:/admin/permissions";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/permissions/replace?roleUuid=" + roleUuid;
        }
    }

    private void fillPermissionFormOptions(Model model, UUID roleUuid) {
        model.addAttribute("roles", roleService.getAll());
        model.addAttribute("allPermissionCodes", Arrays.stream(Permission.values()).map(Enum::name).toList());
        model.addAttribute("selectedRoleUuid", roleUuid);
        if (roleUuid != null) {
            model.addAttribute("currentPermissions",
                    new java.util.LinkedHashSet<>(permissionService.getPermissionsForRole(roleUuid)));
        } else {
            model.addAttribute("currentPermissions", java.util.Set.of());
        }
    }

    private void fillUserFormOptions(Model model) {
        model.addAttribute("roles", roleService.getAll());
        model.addAttribute("schools", schoolService.getAll());
        model.addAttribute("classes", schoolClassService.getAll());
        model.addAttribute("grades", schoolClassService.listGrades());
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

    private static SchoolRequest buildSchoolRequest(
            String name, String description, String address, String phone,
            String email, String website) {
        SchoolRequest request = new SchoolRequest();
        request.setName(name);
        request.setDescription(description);
        request.setAddress(address);
        request.setPhone(phone);
        request.setEmail(email);
        request.setWebsite(website);
        return request;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value.trim());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String safeMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Unexpected error";
        }
        String msg = ex.getMessage().trim();
        return msg.length() > 240 ? msg.substring(0, 240) + "…" : msg;
    }

    private static String assessmentUnavailableMessage(Exception ex) {
        return "Assessment service unavailable — start it on port 8081 "
                + "(docker compose up assessment-service, or mvn -f assessment-service/pom.xml spring-boot:run). "
                + safeMessage(ex);
    }
}
