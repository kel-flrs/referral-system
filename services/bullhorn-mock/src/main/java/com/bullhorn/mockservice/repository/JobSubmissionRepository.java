package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.JobSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for JobSubmission entity
 */
@Repository
public interface JobSubmissionRepository extends JpaRepository<JobSubmission, Long> {

    @Query("SELECT js FROM JobSubmission js WHERE js.candidate.id = :candidateId")
    List<JobSubmission> findByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT js FROM JobSubmission js WHERE js.jobOrder.id = :jobOrderId")
    Page<JobSubmission> findByJobOrderId(@Param("jobOrderId") Long jobOrderId, Pageable pageable);

    @Query("SELECT js FROM JobSubmission js WHERE js.sendingConsultant.id = :consultantId")
    Page<JobSubmission> findByConsultantId(@Param("consultantId") Long consultantId, Pageable pageable);

    @Query("SELECT COUNT(js) > 0 FROM JobSubmission js WHERE js.candidate.id = :candidateId AND js.jobOrder.id = :jobOrderId")
    boolean existsByCandidateIdAndJobOrderId(@Param("candidateId") Long candidateId, @Param("jobOrderId") Long jobOrderId);
}
