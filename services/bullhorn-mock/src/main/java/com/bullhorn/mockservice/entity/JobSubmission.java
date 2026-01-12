package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a job submission (candidate referral to a job)
 */
@Entity
@Table(name = "job_submissions", indexes = {
        @Index(name = "idx_submission_candidate", columnList = "candidate_id"),
        @Index(name = "idx_submission_job_order", columnList = "job_order_id"),
        @Index(name = "idx_submission_consultant", columnList = "sending_consultant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_order_id", nullable = false)
    private JobOrder jobOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sending_consultant_id", nullable = false)
    private Consultant sendingConsultant;

    @Column(name = "status")
    private String status;

    @Column(name = "source")
    private String source;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
