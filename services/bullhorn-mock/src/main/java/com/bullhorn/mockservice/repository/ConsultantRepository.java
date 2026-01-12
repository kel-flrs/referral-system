package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.Consultant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Consultant entity
 */
@Repository
public interface ConsultantRepository extends JpaRepository<Consultant, Long> {

    Optional<Consultant> findByBullhornId(String bullhornId);

    Optional<Consultant> findByEmail(String email);

    List<Consultant> findByIsActive(Boolean isActive);

    Page<Consultant> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT c FROM Consultant c WHERE c.isActive = true")
    Page<Consultant> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Consultant c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Consultant> searchConsultants(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM Consultant c WHERE c.email = :email AND c.id != :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
}
