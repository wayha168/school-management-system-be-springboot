package com.project.school_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.project.school_management.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers(
                                                "/api/v1/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/ws/**"))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/login",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/assets/**",
                                                                "/favicon.ico",
                                                                "/favicon.svg",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/api/v1/auth/**")
                                                .permitAll()
                                                // Public school display: info + images (no login)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/v1/schools",
                                                                "/api/v1/schools/**")
                                                .permitAll()
                                                .requestMatchers("/ws/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/schools/**",
                                                                "/api/v1/users/**", "/api/v1/classes/**",
                                                                "/api/v1/roles/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/schools/**",
                                                                "/api/v1/users/**", "/api/v1/classes/**",
                                                                "/api/v1/roles/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/schools/**",
                                                                "/api/v1/users/**", "/api/v1/classes/**",
                                                                "/api/v1/roles/**")
                                                .authenticated()
                                                .requestMatchers("/api/v1/**")
                                                .authenticated()
                                                .requestMatchers("/admin/**", "/dashboard", "/")
                                                .hasAnyRole("SUPERADMIN", "ADMIN", "PRINCIPAL", "STAFF", "TEACHER", "STUDENT")
                                                .anyRequest()
                                                .authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/admin/dashboard", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
