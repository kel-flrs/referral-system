package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.RestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RestSessionRepository extends JpaRepository<RestSession, Long> {

    Optional<RestSession> findBySessionTokenAndActiveTrue(String sessionToken);

    @Modifying
    @Query("UPDATE RestSession s SET s.active = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(LocalDateTime now);

    @Modifying
    @Query("UPDATE RestSession s SET s.active = false WHERE s.sessionToken = :sessionToken")
    void invalidateSession(String sessionToken);
}
