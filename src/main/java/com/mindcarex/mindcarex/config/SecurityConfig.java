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

        // ── Allow all origins (development mode) ──────────────────────────────
        // TODO: Before production, replace with your actual Vercel URL:
        //   config.setAllowedOrigins(List.of("https://inner-harmony-hub-xi.vercel.app"));
        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        // allowCredentials(true) works with setAllowedOriginPatterns but NOT with setAllowedOrigins("*")
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
