package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Candidate entity
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByBullhornId(String bullhornId);

    Optional<Candidate> findByEmail(String email);

    List<Candidate> findByStatus(String status);

    Page<Candidate> findByStatus(String status, Pageable pageable);

    // Optimized: Use EntityGraph to avoid N+1 queries and MultipleBagFetchException
    @EntityGraph(attributePaths = {"experience", "education", "skills"})
    @Query("SELECT c FROM Candidate c WHERE c.isDeleted = false")
    Page<Candidate> findAllActive(Pageable pageable);

    @Query("""
        SELECT c
        FROM Candidate c
        WHERE c.isDeleted = false
        ORDER BY c.id
    """)
    Page<Candidate> findAllActiveCandidates(Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE c.isDeleted = false AND c.updatedAt > :modifiedSince")
    List<Candidate> findByModifiedSince(@Param("modifiedSince") LocalDateTime modifiedSince);

    @Query("SELECT c FROM Candidate c WHERE c.isDeleted = false AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.currentTitle) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Candidate> searchCandidates(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM Candidate c WHERE c.email = :email AND c.id != :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
}
