package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a Bullhorn Job Order
 */
@Entity
@Table(name = "job_orders", indexes = {
        @Index(name = "idx_job_order_bullhorn_id", columnList = "bullhorn_id"),
        @Index(name = "idx_job_order_status", columnList = "status"),
        @Index(name = "idx_job_order_client", columnList = "client_bullhorn_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOrder extends BaseEntity {

    @Column(name = "bullhorn_id", unique = true, nullable = false)
    private String bullhornId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "employment_type")
    private String employmentType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_order_required_skills", joinColumns = @JoinColumn(name = "job_order_id"))
    @Column(name = "skill")
    @Builder.Default
    private Set<String> requiredSkills = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_order_preferred_skills", joinColumns = @JoinColumn(name = "job_order_id"))
    @Column(name = "skill")
    @Builder.Default
    private Set<String> preferredSkills = new HashSet<>();

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "location")
    private String location;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_bullhorn_id")
    private String clientBullhornId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
