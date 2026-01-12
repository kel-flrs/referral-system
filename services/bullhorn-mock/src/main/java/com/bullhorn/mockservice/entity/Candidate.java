package com.bullhorn.mockservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a Bullhorn Candidate
 */
@Entity
@Table(name = "candidates", indexes = {
        @Index(name = "idx_candidate_bullhorn_id", columnList = "bullhorn_id"),
        @Index(name = "idx_candidate_email", columnList = "email"),
        @Index(name = "idx_candidate_status", columnList = "status"),
        @Index(name = "idx_candidate_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate extends BaseEntity {

    @Column(name = "bullhorn_id", unique = true, nullable = false)
    private String bullhornId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "current_title")
    private String currentTitle;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "location")
    private String location;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    // Helper methods for bidirectional relationships
    public void addExperience(Experience exp) {
        experience.add(exp);
        exp.setCandidate(this);
    }

    public void removeExperience(Experience exp) {
        experience.remove(exp);
        exp.setCandidate(null);
    }

    public void addEducation(Education edu) {
        education.add(edu);
        edu.setCandidate(this);
    }

    public void removeEducation(Education edu) {
        education.remove(edu);
        edu.setCandidate(null);
    }
}
