package com.bullhorn.mockservice.config;

import com.bullhorn.mockservice.entity.OAuthClient;
import com.bullhorn.mockservice.repository.OAuthClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialize sample OAuth clients for testing
 * Runs before DataInitializer (Order = 1)
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class OAuthClientInitializer implements ApplicationRunner {

    private final OAuthClientRepository oAuthClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing OAuth clients...");

        // Check if clients already exist
        if (oAuthClientRepository.count() > 0) {
            log.info("OAuth clients already initialized. Skipping.");
            return;
        }

        // Create sample client 1 - Full access
        OAuthClient client1 = OAuthClient.builder()
                .clientId("test-client-1")
                .clientSecret(passwordEncoder.encode("test-secret-1"))
                .clientName("Test Client Application 1")
                .username("admin@bullhorn.local")
                .password(passwordEncoder.encode("password123"))
                .scopes("read:candidates,write:candidates,read:consultants,write:consultants,read:jobs,write:jobs")
                .enabled(true)
                .accessTokenValiditySeconds(600) // 10 minutes
                .refreshTokenValiditySeconds(86400) // 24 hours
                .build();

        // Create sample client 2 - Read-only access
        OAuthClient client2 = OAuthClient.builder()
                .clientId("reporting-service")
                .clientSecret(passwordEncoder.encode("reporting-secret-456"))
                .clientName("Reporting Service (Read-Only)")
                .username("reports@bullhorn.local")
                .password(passwordEncoder.encode("reports123"))
                .scopes("read:candidates,read:consultants,read:jobs")
                .enabled(true)
                .accessTokenValiditySeconds(600)
                .refreshTokenValiditySeconds(86400)
                .build();

        // Create sample client 3 - Integration service
        OAuthClient client3 = OAuthClient.builder()
                .clientId("integration-service")
                .clientSecret(passwordEncoder.encode("integration-secret-789"))
                .clientName("External Integration Service")
                .username("integration@bullhorn.local")
                .password(passwordEncoder.encode("integration123"))
                .scopes("read:candidates,write:candidates,read:jobs,write:jobs")
                .enabled(true)
                .accessTokenValiditySeconds(600)
                .refreshTokenValiditySeconds(172800) // 48 hours
                .build();

        oAuthClientRepository.save(client1);
        oAuthClientRepository.save(client2);
        oAuthClientRepository.save(client3);

        log.info("Created 3 sample OAuth clients:");
        log.info("  1. test-client-1 (admin@bullhorn.local / password123) - Full Access");
        log.info("  2. reporting-service (reports@bullhorn.local / reports123) - Read-Only");
        log.info("  3. integration-service (integration@bullhorn.local / integration123) - Read/Write");
        log.info("OAuth client initialization completed");
    }
}
