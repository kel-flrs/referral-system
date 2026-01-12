package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.JobOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JobOrder entity
 */
@Repository
public interface JobOrderRepository extends JpaRepository<JobOrder, Long> {

    Optional<JobOrder> findByBullhornId(String bullhornId);

    List<JobOrder> findByStatus(String status);

    Page<JobOrder> findByStatus(String status, Pageable pageable);

    // NOTE: Avoid fetch-joining multiple @ElementCollection Lists (bags).
    // Hibernate can throw MultipleBagFetchException, which surfaces as HTTP 500s.
    @Query("""
        SELECT j
        FROM JobOrder j
        WHERE j.isDeleted = false
    """)
    Page<JobOrder> findAllActive(Pageable pageable);

    @Query(value = """
        SELECT job_order_id, skill
        FROM job_order_required_skills
        WHERE job_order_id IN (:jobOrderIds)
        ORDER BY job_order_id
    """, nativeQuery = true)
    List<Object[]> findRequiredSkillsByJobOrderIds(@Param("jobOrderIds") List<Long> jobOrderIds);

    @Query(value = """
        SELECT job_order_id, skill
        FROM job_order_preferred_skills
        WHERE job_order_id IN (:jobOrderIds)
        ORDER BY job_order_id
    """, nativeQuery = true)
    List<Object[]> findPreferredSkillsByJobOrderIds(@Param("jobOrderIds") List<Long> jobOrderIds);

    @Query("SELECT j FROM JobOrder j WHERE j.isDeleted = false AND j.status = 'OPEN'")
    Page<JobOrder> findAllOpenJobs(Pageable pageable);

    @Query("SELECT j FROM JobOrder j WHERE j.clientBullhornId = :clientId AND j.isDeleted = false")
    List<JobOrder> findByClientBullhornId(@Param("clientId") String clientId);

    @Query("SELECT j FROM JobOrder j WHERE j.isDeleted = false AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.location) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(j.clientName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<JobOrder> searchJobOrders(@Param("query") String query, Pageable pageable);
}
