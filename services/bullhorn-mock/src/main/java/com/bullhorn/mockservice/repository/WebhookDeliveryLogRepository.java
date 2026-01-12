package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.WebhookDeliveryLog;
import com.bullhorn.mockservice.entity.WebhookSubscription;
import com.bullhorn.mockservice.webhook.WebhookDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WebhookDeliveryLog entity
 */
@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, Long> {

    /**
     * Find delivery log by event ID (for idempotency check)
     */
    Optional<WebhookDeliveryLog> findByEventId(String eventId);

    /**
     * Find logs for a specific subscription
     */
    Page<WebhookDeliveryLog> findBySubscription(WebhookSubscription subscription, Pageable pageable);

    /**
     * Find failed deliveries that should be retried
     */
    @Query("SELECT wdl FROM WebhookDeliveryLog wdl " +
           "WHERE wdl.status = 'RETRYING' " +
           "AND wdl.attemptNumber < wdl.maxAttempts " +
           "AND wdl.nextRetryAt <= :now")
    List<WebhookDeliveryLog> findPendingRetries(@Param("now") LocalDateTime now);

    /**
     * Find logs by status
     */
    Page<WebhookDeliveryLog> findByStatus(WebhookDeliveryStatus status, Pageable pageable);

    /**
     * Delete old logs (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Get delivery statistics for a subscription
     */
    @Query("SELECT " +
           "COUNT(wdl) as total, " +
           "SUM(CASE WHEN wdl.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
           "SUM(CASE WHEN wdl.status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
           "AVG(wdl.durationMs) as avgDuration " +
           "FROM WebhookDeliveryLog wdl " +
           "WHERE wdl.subscription.id = :subscriptionId")
    Object[] getDeliveryStats(@Param("subscriptionId") Long subscriptionId);
}
