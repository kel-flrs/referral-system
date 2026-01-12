package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a Bullhorn Consultant (Corporate User)
 */
@Entity
@Table(name = "consultants", indexes = {
        @Index(name = "idx_consultant_bullhorn_id", columnList = "bullhorn_id"),
        @Index(name = "idx_consultant_email", columnList = "email"),
        @Index(name = "idx_consultant_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultant extends BaseEntity {

    @Column(name = "bullhorn_id", unique = true, nullable = false)
    private String bullhornId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "total_placements")
    @Builder.Default
    private Integer totalPlacements = 0;

    @Column(name = "total_referrals")
    @Builder.Default
    private Integer totalReferrals = 0;
}
