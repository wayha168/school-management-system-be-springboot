package com.project.school_management.controller.view;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import com.project.school_management.dto.permission.PermissionAssignRequest;
import com.project.school_management.dto.permission.RolePermissionRequest;
import com.project.school_management.dto.role.RoleRequest;
import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.schoolclass.SchoolClassRequest;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserRequest;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.dto.user.UserUpdateRequest;
import com.project.school_management.enums.Permission;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ErrorRuntime;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.service.permission.PermissionService;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.role.RoleService;
import com.project.school_management.service.school.SchoolService;
import com.project.school_management.service.schoolclass.SchoolClassService;
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

    public AdminViewController(
            SchoolService schoolService,
            UserService userService,
            SchoolClassService schoolClassService,
            RoleService roleService,
            PermissionService permissionService,
            UserRepository userRepository,
            PresenceTracker presenceTracker) {
        this.schoolService = schoolService;
        this.userService = userService;
        this.schoolClassService = schoolClassService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.presenceTracker = presenceTracker;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            fillCommon(model, authentication, "dashboard", "Dashboard");
            model.addAttribute("schoolCount", schoolService.getAll().size());
            model.addAttribute("userCount", userService.getAll().size());
            model.addAttribute("classCount", schoolClassService.getAll().size());
            model.addAttribute("roleCount", roleService.getAll().size());
            model.addAttribute("teacherCount", userRepository.countByRole_Name(RoleName.TEACHER));
            model.addAttribute("studentCount", userRepository.countByRole_Name(RoleName.STUDENT));
            model.addAttribute("staffCount", userRepository.countByRole_Name(RoleName.STAFF));
            model.addAttribute("permissionCount", Permission.values().length);
            model.addAttribute("onlineCount", presenceTracker.snapshot().getOnlineCount());
            return "pages/dashboard";
        } catch (UserNotFound | ErrorRuntime ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/login?error";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Unable to load dashboard");
            return "redirect:/login?error";
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

    @PostMapping("/schools")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String createSchool(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String address,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String website,
            @RequestParam(defaultValue = "/logo.png") String logo,
            @RequestParam(defaultValue = "/banner.png") String banner,
            RedirectAttributes ra) {
        try {
            SchoolRequest request = buildSchoolRequest(name, description, address, phone, email, website, logo, banner);
            schoolService.create(request);
            ra.addFlashAttribute("success", "School created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools/new";
        }
        return "redirect:/admin/schools";
    }

    @PostMapping("/schools/{id}/update")
    @PreAuthorize("hasAuthority('SCHOOL_WRITE')")
    public String updateSchool(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String address,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String website,
            @RequestParam(defaultValue = "/logo.png") String logo,
            @RequestParam(defaultValue = "/banner.png") String banner,
            RedirectAttributes ra) {
        try {
            SchoolRequest request = buildSchoolRequest(name, description, address, phone, email, website, logo, banner);
            schoolService.update(id, request);
            ra.addFlashAttribute("success", "School updated");
            return "redirect:/admin/schools/" + id;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/schools/" + id + "/edit";
        }
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
    public String users(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "users", "Users");
            model.addAttribute("users", userService.getAll());
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
        model.addAttribute("roles", roleService.getAll());
        model.addAttribute("schools", schoolService.getAll());
        model.addAttribute("classes", schoolClassService.getAll());
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
            model.addAttribute("roles", roleService.getAll());
            model.addAttribute("schools", schoolService.getAll());
            model.addAttribute("classes", schoolClassService.getAll());
            model.addAttribute("mode", "edit");
            return "pages/user-form";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String createUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UUID roleUuid,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) String classUuid,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String room,
            RedirectAttributes ra) {
        try {
            UserRequest request = new UserRequest();
            request.setName(name);
            request.setEmail(email);
            request.setPassword(password);
            request.setRoleUuid(roleUuid);
            request.setSchoolUuid(schoolUuid);
            request.setClassUuid(parseUuid(classUuid));
            request.setGrade(grade);
            request.setRoom(room);
            userService.create(request);
            ra.addFlashAttribute("success", "User created");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/update")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public String updateUser(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam UUID roleUuid,
            @RequestParam UUID schoolUuid,
            @RequestParam(required = false) String classUuid,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String room,
            RedirectAttributes ra) {
        try {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setName(name);
            request.setEmail(email);
            request.setPassword(blankToNull(password));
            request.setRoleUuid(roleUuid);
            request.setSchoolUuid(schoolUuid);
            request.setClassUuid(parseUuid(classUuid));
            request.setGrade(grade);
            request.setRoom(room);
            userService.update(id, request);
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

    // ── Classes ──────────────────────────────────────────────────────────────

    @GetMapping("/classes")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    public String classes(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Classes");
            model.addAttribute("classes", schoolClassService.getAll());
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
        model.addAttribute("mode", "create");
        return "pages/class-form";
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    public String classView(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "classes", "Class");
            model.addAttribute("item", schoolClassService.getById(id));
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
            @RequestParam UUID schoolUuid,
            RedirectAttributes ra) {
        try {
            SchoolClassRequest request = new SchoolClassRequest();
            request.setName(name);
            request.setGrade(blankToNull(grade));
            request.setSchoolUuid(schoolUuid);
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
            @RequestParam UUID schoolUuid,
            RedirectAttributes ra) {
        try {
            SchoolClassRequest request = new SchoolClassRequest();
            request.setName(name);
            request.setGrade(blankToNull(grade));
            request.setSchoolUuid(schoolUuid);
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
            model.addAttribute("roles", roleService.getAll());
            model.addAttribute("allPermissionCodes", Arrays.stream(Permission.values()).map(Enum::name).toList());
            return "pages/permissions";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load permissions");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/permissions/assign")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String assignPermission(
            @RequestParam UUID roleUuid,
            @RequestParam String permission,
            RedirectAttributes ra) {
        try {
            PermissionAssignRequest request = new PermissionAssignRequest();
            request.setRoleUuid(roleUuid);
            request.setPermission(permission);
            permissionService.assign(request);
            ra.addFlashAttribute("success", "Permission assigned");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/permissions";
    }

    @PostMapping("/permissions/revoke")
    @PreAuthorize("hasAuthority('ROLES_WRITE')")
    public String revokePermission(
            @RequestParam UUID roleUuid,
            @RequestParam String permission,
            RedirectAttributes ra) {
        try {
            permissionService.revoke(roleUuid, permission);
            ra.addFlashAttribute("success", "Permission revoked");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/permissions";
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
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/permissions";
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
            String email, String website, String logo, String banner) {
        SchoolRequest request = new SchoolRequest();
        request.setName(name);
        request.setDescription(description);
        request.setAddress(address);
        request.setPhone(phone);
        request.setEmail(email);
        request.setWebsite(website);
        request.setLogo(blankTo(logo, "/logo.png"));
        request.setBanner(blankTo(banner, "/banner.png"));
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
}
