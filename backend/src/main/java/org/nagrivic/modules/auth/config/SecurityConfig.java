package org.nagrivic.modules.auth.config;

import org.nagrivic.modules.auth.security.JwtAuthenticationEntryPoint;
import org.nagrivic.modules.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/send-otp",
                                "/api/auth/verify-otp",
                                "/api/auth/google",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()
                        .requestMatchers(
                                "/api/health",
                                "/api/health/**",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        // Authenticated citizen's own reports
                        .requestMatchers(HttpMethod.GET, "/api/issues/my").authenticated()
                        // Public read-only issue viewing and categories
                        .requestMatchers(HttpMethod.GET, "/api/issues", "/api/issues/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media", "/api/media/**").permitAll()
                        // Citizen reporting of content
                        .requestMatchers(HttpMethod.POST, "/api/moderation/reports").authenticated()
                        // Civic geography & responsibility configuration requires ADMIN only
                        .requestMatchers("/api/admin/geography/**").hasRole("ADMIN")
                        // Privileged moderation & admin operations require MODERATOR, ADMIN, or legacy OFFICER
                        .requestMatchers("/api/moderation/**", "/api/admin/**").hasAnyRole("MODERATOR", "ADMIN", "OFFICER")
                        // Authority operational issue management requires OFFICER or ADMIN
                        .requestMatchers("/api/authority/**").hasAnyRole("OFFICER", "ADMIN")
                        // POST /api/issues and all other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
