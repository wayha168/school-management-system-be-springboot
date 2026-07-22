package com.project.assignment.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AssignmentAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_CALLER = "assignment.caller";

    private final JwtSupport jwtSupport;
    private final String internalKey;

    public AssignmentAuthFilter(
            JwtSupport jwtSupport,
            @Value("${app.internal-key}") String internalKey) {
        this.jwtSupport = jwtSupport;
        this.internalKey = internalKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")
                || path.equals("/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        CallerContext caller = resolveCaller(request);
        if (caller.userUuid() == null && caller.email() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setAttribute(ATTR_CALLER, caller);
        List<SimpleGrantedAuthority> authorities = caller.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                caller.email() != null ? caller.email() : String.valueOf(caller.userUuid()),
                null,
                authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    private CallerContext resolveCaller(HttpServletRequest request) {
        String internal = request.getHeader("X-Internal-Key");
        if (internal != null && internal.equals(internalKey)) {
            return fromHeaders(request);
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtSupport.parse(header.substring(7));
                if (jwtSupport.isExpired(claims)) {
                    return CallerContext.empty();
                }
                Set<String> authorities = new HashSet<>(jwtSupport.authorities(claims));
                return new CallerContext(
                        jwtSupport.userUuid(claims),
                        claims.getSubject(),
                        jwtSupport.role(claims),
                        authorities,
                        jwtSupport.schoolUuid(claims));
            } catch (Exception ex) {
                return CallerContext.empty();
            }
        }
        return CallerContext.empty();
    }

    private static CallerContext fromHeaders(HttpServletRequest request) {
        UUID userUuid = parseUuid(request.getHeader("X-User-Uuid"));
        String email = request.getHeader("X-User-Email");
        String role = request.getHeader("X-User-Role");
        UUID schoolUuid = parseUuid(request.getHeader("X-School-Uuid"));
        String authHeader = request.getHeader("X-Authorities");
        Set<String> authorities = authHeader == null || authHeader.isBlank()
                ? Set.of()
                : Arrays.stream(authHeader.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
        if (role != null && !role.isBlank()) {
            authorities = new HashSet<>(authorities);
            authorities.add("ROLE_" + role.trim().toUpperCase());
        }
        return new CallerContext(userUuid, email, role, authorities, schoolUuid);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
