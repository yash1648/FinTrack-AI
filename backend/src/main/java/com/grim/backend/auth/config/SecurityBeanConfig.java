package com.grim.backend.auth.config;

import com.grim.backend.auth.security.CustomUserDetailsService;
import com.grim.backend.auth.security.JwtAuthenticationFilter;
import com.grim.backend.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityBeanConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilterChain) throws Exception {
        log.info("Security Filter Chain");
        http
                .csrf(csrf->csrf.disable())
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/swagger/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/logout",
                                 "/api/v1/auth/profile",
                                 "/api/v1/auth/change-password"
                         ).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilterChain,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }




}
