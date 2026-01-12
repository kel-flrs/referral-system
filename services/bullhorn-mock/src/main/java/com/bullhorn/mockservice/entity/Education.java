package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents education background for a candidate
 */
@Entity
@Table(name = "educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "education_seq_gen")
    @SequenceGenerator(name = "education_seq_gen", sequenceName = "education_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "school", nullable = false)
    private String school;

    @Column(name = "degree", nullable = false)
    private String degree;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "field_of_study")
    private String fieldOfStudy;
}
