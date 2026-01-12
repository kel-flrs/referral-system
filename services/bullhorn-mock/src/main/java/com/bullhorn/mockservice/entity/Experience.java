package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents work experience for a candidate
 */
@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "experience_seq_gen")
    @SequenceGenerator(name = "experience_seq_gen", sequenceName = "experience_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
