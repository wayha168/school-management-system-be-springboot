package com.project.school_management.service.auth;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.project.school_management.dto.auth.LoginRequest;
import com.project.school_management.dto.auth.LoginResponse;
import com.project.school_management.entities.User;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.jwt.JwtUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findDetailedByEmail(principal.getUsername()).orElse(null);
        String token = jwtUtils.generateToken(principal, user);

        String role = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse("UNKNOWN");

        List<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .toList();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(principal.getUsername())
                .role(role)
                .permissions(permissions)
                .build();
    }
}
