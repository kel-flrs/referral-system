package com.bullhorn.mockservice.repository;

import com.bullhorn.mockservice.entity.WebhookSubscription;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WebhookSubscription entity
 */
@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    /**
     * Find all active subscriptions for a specific event type
     */
    List<WebhookSubscription> findByEventTypeAndActiveTrue(WebhookEventType eventType);

    /**
     * Find all active subscriptions (includes wildcard subscriptions)
     */
    List<WebhookSubscription> findByActiveTrue();

    /**
     * Find subscriptions by subscriber email
     */
    List<WebhookSubscription> findBySubscriberEmail(String email);

    /**
     * Find active subscriptions for an event type (including wildcard)
     */
    @Query("SELECT ws FROM WebhookSubscription ws " +
           "WHERE ws.active = true " +
           "AND (ws.eventType = :eventType OR ws.eventType = 'ALL')")
    List<WebhookSubscription> findActiveSubscriptionsForEvent(@Param("eventType") WebhookEventType eventType);

    /**
     * Check if a callback URL already exists for a subscriber
     */
    Optional<WebhookSubscription> findBySubscriberEmailAndCallbackUrl(String email, String callbackUrl);

    /**
     * Count active subscriptions
     */
    long countByActiveTrue();
}
