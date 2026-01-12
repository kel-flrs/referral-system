package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    @Query("""
        SELECT e
        FROM Experience e
        WHERE e.candidate.id IN :candidateIds
        ORDER BY e.candidate.id, e.startDate
    """)
    List<Experience> findByCandidateIds(List<Long> candidateIds);
}
