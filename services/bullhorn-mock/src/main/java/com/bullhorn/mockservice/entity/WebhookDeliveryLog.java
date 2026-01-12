package com.bullhorn.mockservice.entity;

import com.bullhorn.mockservice.webhook.WebhookDeliveryStatus;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing webhook delivery attempts and their outcomes
 * Used for debugging, monitoring, and retry logic
 */
@Entity
@Table(name = "webhook_delivery_logs", indexes = {
    @Index(name = "idx_webhook_log_subscription", columnList = "subscription_id"),
    @Index(name = "idx_webhook_log_status", columnList = "status"),
    @Index(name = "idx_webhook_log_event_id", columnList = "event_id"),
    @Index(name = "idx_webhook_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_log_seq_gen")
    @SequenceGenerator(name = "webhook_log_seq_gen", sequenceName = "webhook_delivery_log_sequence", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WebhookSubscription subscription;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;  // UUID for idempotency

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private WebhookEventType eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;  // JSON payload sent

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookDeliveryStatus status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "duration_ms")
    private Long durationMs;  // How long the request took

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // Helper methods
    public boolean shouldRetry() {
        return status == WebhookDeliveryStatus.RETRYING
            && attemptNumber < maxAttempts
            && nextRetryAt != null
            && LocalDateTime.now().isAfter(nextRetryAt);
    }

    public void markAsDelivered(int statusCode, String response, long duration) {
        this.status = WebhookDeliveryStatus.DELIVERED;
        this.httpStatusCode = statusCode;
        this.responseBody = response;
        this.durationMs = duration;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = WebhookDeliveryStatus.FAILED;
        this.errorMessage = error;
    }

    public void scheduleRetry(long delaySeconds) {
        this.status = WebhookDeliveryStatus.RETRYING;
        this.attemptNumber++;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
