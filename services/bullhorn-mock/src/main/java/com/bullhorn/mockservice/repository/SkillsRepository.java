package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillsRepository extends JpaRepository<Candidate, Long> {

    @Query(value = """
        SELECT candidate_id, skill
        FROM candidate_skills
        WHERE candidate_id IN (:candidateIds)
        ORDER BY candidate_id
    """, nativeQuery = true)
    List<Object[]> findSkillsByCandidateIds(List<Long> candidateIds);
}

