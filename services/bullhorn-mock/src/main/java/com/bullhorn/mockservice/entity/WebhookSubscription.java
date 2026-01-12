package com.bullhorn.mockservice.entity;

import com.bullhorn.mockservice.webhook.WebhookEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a webhook subscription
 * Consumers register their callback URLs to receive real-time notifications
 */
@Entity
@Table(name = "webhook_subscriptions", indexes = {
    @Index(name = "idx_webhook_event_type", columnList = "event_type"),
    @Index(name = "idx_webhook_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_sub_seq_gen")
    @SequenceGenerator(name = "webhook_sub_seq_gen", sequenceName = "webhook_subscription_sequence", allocationSize = 50)
    private Long id;

    @Column(name = "subscriber_name", nullable = false)
    private String subscriberName;

    @Column(name = "subscriber_email", nullable = false)
    private String subscriberEmail;

    @Column(name = "callback_url", nullable = false, length = 500)
    private String callbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private WebhookEventType eventType;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;  // For HMAC signature verification

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "description")
    private String description;

    // Rate limiting
    @Column(name = "max_events_per_minute")
    @Builder.Default
    private Integer maxEventsPerMinute = 60;  // Default: 60 events/min

    // Retry configuration
    @Column(name = "max_retry_attempts")
    @Builder.Default
    private Integer maxRetryAttempts = 5;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    // Statistics
    @Column(name = "total_deliveries")
    @Builder.Default
    private Long totalDeliveries = 0L;

    @Column(name = "failed_deliveries")
    @Builder.Default
    private Long failedDeliveries = 0L;

    @Column(name = "last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public void incrementDeliveries() {
        this.totalDeliveries++;
        this.lastDeliveryAt = LocalDateTime.now();
    }

    public void incrementFailures() {
        this.failedDeliveries++;
        this.lastFailureAt = LocalDateTime.now();
    }
}
