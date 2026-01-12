package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClient, Long> {

    Optional<OAuthClient> findByClientId(String clientId);

    Optional<OAuthClient> findByClientIdAndEnabled(String clientId, Boolean enabled);

    boolean existsByClientId(String clientId);
}
