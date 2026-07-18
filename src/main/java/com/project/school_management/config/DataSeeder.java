package com.project.school_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    public DataSeeder(
            RoleRepository roleRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        SchoolMag school = seedDefaultSchool();
        seedAdmin(school);
    }

    private void seedRoles() {
        seedRole(RoleName.ADMIN, "Administrator");
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

    private void seedAdmin(SchoolMag school) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing"));
        User admin = new User("System Admin", adminEmail, passwordEncoder.encode(adminPassword), adminRole, school);
        userRepository.save(admin);
    }
}
