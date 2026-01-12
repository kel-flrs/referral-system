package com.bullhorn.mockservice.dto.webhook;

import com.bullhorn.mockservice.webhook.WebhookEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for webhook subscription
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionResponse {

    private Long id;
    private String subscriberName;
    private String subscriberEmail;
    private String callbackUrl;
    private WebhookEventType eventType;
    private String secretKey;  // Returned only on creation
    private Boolean active;
    private String description;

    // Configuration
    private Integer maxEventsPerMinute;
    private Integer maxRetryAttempts;
    private Integer timeoutSeconds;

    // Statistics
    private Long totalDeliveries;
    private Long failedDeliveries;
    private LocalDateTime lastDeliveryAt;
    private LocalDateTime lastFailureAt;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
