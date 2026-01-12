package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Activity;
import com.bullhorn.mockservice.entity.Consultant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Activity entity
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Page<Activity> findByConsultant(Consultant consultant, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.consultant.id = :consultantId")
    Page<Activity> findByConsultantId(@Param("consultantId") Long consultantId, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.consultant.id = :consultantId AND a.activityDate > :since")
    List<Activity> findByConsultantIdAndActivityDateAfter(
            @Param("consultantId") Long consultantId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT a FROM Activity a WHERE a.candidate.id = :candidateId")
    Page<Activity> findByCandidateId(@Param("candidateId") Long candidateId, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.jobOrder.id = :jobOrderId")
    Page<Activity> findByJobOrderId(@Param("jobOrderId") Long jobOrderId, Pageable pageable);

    List<Activity> findByAction(String action);
}
