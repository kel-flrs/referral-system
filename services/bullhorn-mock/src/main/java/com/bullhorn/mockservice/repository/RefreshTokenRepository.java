package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.expiresAt < :now")
    void revokeExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.clientId = :clientId")
    void revokeAllByClientId(String clientId);
}
