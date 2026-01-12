package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {

    @Query("""
        SELECT ed
        FROM Education ed
        WHERE ed.candidate.id IN :candidateIds
        ORDER BY ed.candidate.id, ed.startDate
    """)
    List<Education> findByCandidateIds(List<Long> candidateIds);
}

