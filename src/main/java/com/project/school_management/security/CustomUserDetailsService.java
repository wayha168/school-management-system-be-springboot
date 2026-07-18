package com.project.school_management.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.enums.RoleName;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.service.permission.PermissionService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public CustomUserDetailsService(UserRepository userRepository, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findDetailedByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        RoleName roleName = user.getRole().getName();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.name()));
        permissionService.getPermissionsForRole(roleName).forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission)));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
