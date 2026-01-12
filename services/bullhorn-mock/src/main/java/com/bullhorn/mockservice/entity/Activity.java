package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a consultant activity (note, email, call)
 */
@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_consultant", columnList = "consultant_id"),
        @Index(name = "idx_activity_candidate", columnList = "candidate_id"),
        @Index(name = "idx_activity_date", columnList = "activity_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseEntity {

    @Column(name = "action", nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id", nullable = false)
    private Consultant consultant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_order_id")
    private JobOrder jobOrder;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;
}
