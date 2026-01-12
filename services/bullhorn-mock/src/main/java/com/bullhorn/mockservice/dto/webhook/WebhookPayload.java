package com.bullhorn.mockservice.dto.webhook;

import com.bullhorn.mockservice.webhook.WebhookEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard webhook payload structure sent to subscribers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookPayload<T> {

    /**
     * Unique event ID for idempotency
     */
    private String eventId;

    /**
     * Type of event (e.g., candidate.created)
     */
    private WebhookEventType eventType;

    /**
     * When the event occurred
     */
    private LocalDateTime timestamp;

    /**
     * API version
     */
    @Builder.Default
    private String apiVersion = "v1";

    /**
     * The actual data payload
     */
    private T data;

    /**
     * Additional metadata
     */
    private WebhookMetadata metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebhookMetadata {
        /**
         * ID of the subscription that triggered this webhook
         */
        private Long subscriptionId;

        /**
         * Attempt number (for retries)
         */
        private Integer attempt;

        /**
         * Environment (dev, staging, production)
         */
        private String environment;
    }
}
