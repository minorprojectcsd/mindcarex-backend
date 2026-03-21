package com.mindcarex.mindcarex.config;

import com.mindcarex.mindcarex.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // ✅ Allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Public endpoints
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // ✅ Health check
                        .requestMatchers("/health", "/actuator/**").permitAll()

                        // ✅ WebSocket endpoints
                        .requestMatchers("/ws/**", "/topic/**", "/app/**").permitAll()

                        // ✅ Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ✅ Email statistics (Doctor + Admin)
                        .requestMatchers("/api/notifications/statistics")
                        .hasAnyRole("DOCTOR", "ADMIN")

                        // ✅ Doctor-only endpoints
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")

                        // ✅ Patient-only endpoints
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")

                        // ✅ Authenticated endpoints
                        .requestMatchers(
                                "/api/appointments/**",
                                "/api/users/**",
                                "/api/sessions/**",
                                "/api/profile/**",
                                "/api/notifications/history",
                                "/api/notifications/test"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // ── Read allowed origins from env var ─────────────────────────────────
        // Set in Render: ALLOWED_ORIGINS=https://mindcarex.vercel.app,https://inner-harmony-hub-xi.vercel.app,http://localhost:5173
        // Falls back to localhost only if env var not set
        String allowedOriginsEnv = System.getenv("ALLOWED_ORIGINS");

        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            // Use setAllowedOriginPatterns so credentials=true works with specific URLs
            config.setAllowedOriginPatterns(
                Arrays.asList(allowedOriginsEnv.split(","))
            );
        } else {
            // Local dev fallback
            config.setAllowedOriginPatterns(List.of("http://localhost:5173"));
        }

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        // Works with setAllowedOriginPatterns (not setAllowedOrigins)
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
