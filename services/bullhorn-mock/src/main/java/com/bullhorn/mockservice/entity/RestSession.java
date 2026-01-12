package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * REST Session entity representing active API sessions
 * Matches Bullhorn's session-based API access model
 */
@Entity
@Table(name = "rest_sessions", indexes = {
    @Index(name = "idx_session_token", columnList = "sessionToken"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rest_session_seq_gen")
    @SequenceGenerator(name = "rest_session_seq_gen", sequenceName = "rest_session_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionToken; // BhRestToken

    @Column(nullable = false)
    private String restUrl; // Base REST URL for this session

    @Column(nullable = false)
    private String clientId; // Which OAuth client owns this session

    @Column(nullable = false)
    private String username; // User associated with this session

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
