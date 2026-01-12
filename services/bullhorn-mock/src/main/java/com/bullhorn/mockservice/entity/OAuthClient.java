package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * OAuth Client entity representing registered API consumers
 * Similar to Bullhorn's OAuth client registration
 */
@Entity
@Table(name = "oauth_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oauth_client_seq_gen")
    @SequenceGenerator(name = "oauth_client_seq_gen", sequenceName = "oauth_client_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret; // Stored as BCrypt hash

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String username; // Bullhorn username for this client

    @Column(nullable = false)
    private String password; // Hashed password

    @Column(nullable = false)
    private String scopes; // Comma-separated scopes

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer accessTokenValiditySeconds = 600; // 10 minutes like Bullhorn

    @Column(nullable = false)
    @Builder.Default
    private Integer refreshTokenValiditySeconds = 86400; // 24 hours

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
