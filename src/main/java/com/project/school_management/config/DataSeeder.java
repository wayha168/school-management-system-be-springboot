package com.project.school_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.entities.Role;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.User;
import com.project.school_management.enums.RoleName;
import com.project.school_management.repository.RoleRepository;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.service.permission.PermissionService;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    @Value("${app.seed.superadmin-email}")
    private String superAdminEmail;

    @Value("${app.seed.superadmin-password}")
    private String superAdminPassword;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    public DataSeeder(
            RoleRepository roleRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Old Hibernate enum CHECK blocks new values like SUPERADMIN
        jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
        seedRoles();
        SchoolMag school = seedDefaultSchool();
        seedUser("Super Admin", superAdminEmail, superAdminPassword, RoleName.SUPERADMIN, school);
        seedUser("System Admin", adminEmail, adminPassword, RoleName.ADMIN, school);
        permissionService.seedDefaultsIfEmpty();
    }

    private void seedRoles() {
        seedRole(RoleName.SUPERADMIN, "Platform super administrator");
        seedRole(RoleName.ADMIN, "School administrator");
        seedRole(RoleName.PRINCIPAL, "School principal");
        seedRole(RoleName.TEACHER, "Teaching staff");
        seedRole(RoleName.STUDENT, "Enrolled student");
        seedRole(RoleName.STAFF, "Non-teaching staff");
    }

    private void seedRole(RoleName name, String description) {
        if (!roleRepository.existsByName(name)) {
            roleRepository.save(new Role(name, description));
        }
    }

    private SchoolMag seedDefaultSchool() {
        return schoolRepository.findAll().stream().findFirst().orElseGet(() -> schoolRepository.save(
                new SchoolMag(
                        "Demo School",
                        "Default school for bootstrap",
                        "123 Education St",
                        "000-000-0000",
                        "info@demoschool.com",
                        "https://demoschool.com",
                        "/logo.png",
                        "/banner.png")));
    }

    private void seedUser(String name, String email, String password, RoleName roleName, SchoolMag school) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role missing"));
        userRepository.save(new User(name, email, passwordEncoder.encode(password), role, school));
    }
}
