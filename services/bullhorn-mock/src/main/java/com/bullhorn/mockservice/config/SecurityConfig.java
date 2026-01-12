package com.bullhorn.mockservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for OAuth 2.0 protected API
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    public SecurityConfig(@Lazy SessionAuthenticationFilter sessionAuthenticationFilter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for REST API
            .cors(cors -> {}) // Use CORS filter configuration
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - OAuth flow
                .requestMatchers("/oauth/**").permitAll()
                .requestMatchers("/rest-services/login").permitAll()

                // Public endpoints - Documentation, health, and webhook subscription setup (permitted for ease of integration)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/actuator/health",
                    "/api/v1/webhooks/subscriptions"
                ).permitAll()

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/rest-services/**").authenticated()

                // Default - deny all
                .anyRequest().authenticated()
            )
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
