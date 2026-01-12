package com.bullhorn.mockservice.webhook;

/**
 * Status of webhook delivery attempts
 */
public enum WebhookDeliveryStatus {
    PENDING,      // Queued for delivery
    DELIVERED,    // Successfully delivered
    FAILED,       // All retries exhausted
    RETRYING      // Temporary failure, will retry
}
