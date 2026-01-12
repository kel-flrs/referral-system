package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Refresh Token entity for long-lived token storage
 * Allows clients to get new access tokens without re-authenticating
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_client_id", columnList = "clientId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq_gen")
    @SequenceGenerator(name = "refresh_token_seq_gen", sequenceName = "refresh_token_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String scopes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;
}
