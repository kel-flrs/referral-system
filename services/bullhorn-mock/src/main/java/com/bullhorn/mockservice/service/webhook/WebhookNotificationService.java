package com.bullhorn.mockservice.service.webhook;

import com.bullhorn.mockservice.dto.webhook.WebhookPayload;
import com.bullhorn.mockservice.entity.WebhookDeliveryLog;
import com.bullhorn.mockservice.entity.WebhookSubscription;
import com.bullhorn.mockservice.repository.WebhookDeliveryLogRepository;
import com.bullhorn.mockservice.repository.WebhookSubscriptionRepository;
import com.bullhorn.mockservice.webhook.WebhookDeliveryStatus;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for sending webhook notifications to subscribers
 * Implements retry logic, HMAC signatures, and delivery tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookNotificationService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Notify all subscribers for a specific event
     * This method is async and non-blocking
     */
    @Async("webhookExecutor")
    public void notifySubscribers(WebhookEventType eventType, Object data) {
        List<WebhookSubscription> subscriptions = subscriptionRepository
            .findActiveSubscriptionsForEvent(eventType);

        log.info("Notifying {} subscribers for event: {}", subscriptions.size(), eventType);

        for (WebhookSubscription subscription : subscriptions) {
            try {
                sendWebhook(subscription, eventType, data, 1, null);
            } catch (Exception e) {
                log.error("Error initiating webhook for subscription {}: {}",
                         subscription.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send webhook notification to a single subscription
     */
    @Transactional
    public void sendWebhook(
        WebhookSubscription subscription,
        WebhookEventType eventType,
        Object data,
        int attemptNumber,
        WebhookDeliveryLog existingLog
    ) {

        final String eventId;
        final WebhookDeliveryLog deliveryLog;
        final String jsonPayload;

        if (existingLog != null) {
            // Retry path: reuse existing event ID and payload for idempotency
            eventId = existingLog.getEventId();
            deliveryLog = existingLog;
            deliveryLog.setAttemptNumber(attemptNumber);
            deliveryLog.setStatus(WebhookDeliveryStatus.PENDING);
            deliveryLog.setNextRetryAt(null);
            if (existingLog.getPayload() != null) {
                jsonPayload = existingLog.getPayload();
            } else {
                WebhookPayload<?> payload = WebhookPayload.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .timestamp(LocalDateTime.now())
                    .apiVersion("v1")
                    .data(data)
                    .metadata(WebhookPayload.WebhookMetadata.builder()
                        .subscriptionId(subscription.getId())
                        .attempt(attemptNumber)
                        .environment("production")
                        .build())
                    .build();

                try {
                    jsonPayload = objectMapper.writeValueAsString(payload);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize webhook payload", e);
                }
                deliveryLog.setPayload(jsonPayload);
            }
        } else {
            eventId = UUID.randomUUID().toString();

            // Check for duplicate (idempotency)
            if (deliveryLogRepository.findByEventId(eventId).isPresent()) {
                log.warn("Duplicate event ID detected: {}", eventId);
                return;
            }

            // Build payload once per event
            WebhookPayload<?> payload = WebhookPayload.builder()
                .eventId(eventId)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .apiVersion("v1")
                .data(data)
                .metadata(WebhookPayload.WebhookMetadata.builder()
                    .subscriptionId(subscription.getId())
                    .attempt(attemptNumber)
                    .environment("production")
                    .build())
                .build();

            try {
                jsonPayload = objectMapper.writeValueAsString(payload);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize webhook payload", e);
            }

            deliveryLog = WebhookDeliveryLog.builder()
                .subscription(subscription)
                .eventId(eventId)
                .eventType(eventType)
                .status(WebhookDeliveryStatus.PENDING)
                .attemptNumber(attemptNumber)
                .maxAttempts(subscription.getMaxRetryAttempts())
                .payload(jsonPayload)
                .build();
        }

        try {
            long timestampSeconds = Instant.now().getEpochSecond();

            // Generate timestamped HMAC signature (replay-protected)
            String signatureV2 = generateSignatureHeader(
                jsonPayload,
                subscription.getSecretKey(),
                timestampSeconds
            );

            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signatureV2);
            headers.set("X-Webhook-Timestamp", String.valueOf(timestampSeconds));
            headers.set("X-Webhook-Subscription-Id", String.valueOf(subscription.getId()));
            headers.set("X-Event-Type", eventType.getValue());
            headers.set("X-Event-Id", eventId);
            headers.set("X-Delivery-Attempt", String.valueOf(attemptNumber));
            headers.set("User-Agent", "Bullhorn-Mock-Service-Webhook/1.0");

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            // Send webhook
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = webhookRestTemplate.postForEntity(
                subscription.getCallbackUrl(),
                request,
                String.class
            );
            long duration = System.currentTimeMillis() - startTime;

            // Handle response
            if (response.getStatusCode().is2xxSuccessful()) {
                deliveryLog.markAsDelivered(
                    response.getStatusCode().value(),
                    response.getBody(),
                    duration
                );
                subscription.incrementDeliveries();

                log.info("Webhook delivered successfully to {} for event {} (attempt {}, {}ms)",
                        subscription.getCallbackUrl(), eventType, attemptNumber, duration);
            } else {
                handleFailure(deliveryLog, subscription, response.getStatusCode().value(),
                             "Unexpected status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            handleFailure(deliveryLog, subscription, null, e.getMessage());
        }

        deliveryLogRepository.save(deliveryLog);
        subscriptionRepository.save(subscription);
    }

    /**
     * Handle webhook delivery failure
     */
    private void handleFailure(WebhookDeliveryLog deliveryLog, WebhookSubscription subscription,
                              Integer statusCode, String errorMessage) {

        log.warn("Webhook delivery failed to {} (attempt {}): {}",
                subscription.getCallbackUrl(), deliveryLog.getAttemptNumber(), errorMessage);

        deliveryLog.setHttpStatusCode(statusCode);
        deliveryLog.setErrorMessage(errorMessage);

        if (deliveryLog.getAttemptNumber() < deliveryLog.getMaxAttempts()) {
            // Schedule retry with exponential backoff
            long delaySeconds = calculateBackoffDelay(deliveryLog.getAttemptNumber());
            deliveryLog.scheduleRetry(delaySeconds);

            log.info("Scheduling retry #{} for event {} in {} seconds",
                    deliveryLog.getAttemptNumber() + 1,
                    deliveryLog.getEventId(),
                    delaySeconds);
        } else {
            // All retries exhausted
            deliveryLog.markAsFailed("Max retry attempts exhausted");
            subscription.incrementFailures();

            log.error("Webhook delivery failed permanently for event {} after {} attempts",
                     deliveryLog.getEventId(), deliveryLog.getAttemptNumber());
        }
    }

    /**
     * Calculate exponential backoff delay
     * Attempt 1: 30s
     * Attempt 2: 60s
     * Attempt 3: 120s (2 min)
     * Attempt 4: 300s (5 min)
     * Attempt 5: 600s (10 min)
     */
    private long calculateBackoffDelay(int attempt) {
        long base = (long) Math.min(
            30 * Math.pow(2, attempt - 1),  // Exponential: 30, 60, 120, 240, 480
            600  // Max 10 minutes
        );

        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base / 5));
        return base + jitter;
    }

    /**
     * Process pending retries
     * Runs every minute to check for webhooks that need retry
     */
    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    @Transactional
    public void processRetries() {
        List<WebhookDeliveryLog> pendingRetries = deliveryLogRepository
            .findPendingRetries(LocalDateTime.now());

        if (!pendingRetries.isEmpty()) {
            log.info("Processing {} pending webhook retries", pendingRetries.size());

            for (WebhookDeliveryLog deliveryLog : pendingRetries) {
                try {
                    WebhookSubscription subscription = deliveryLog.getSubscription();

                    sendWebhook(
                        subscription,
                        deliveryLog.getEventType(),
                        null, // payload already stored on delivery log
                        deliveryLog.getAttemptNumber(),
                        deliveryLog
                    );

                } catch (Exception e) {
                    log.error("Error processing retry for delivery log {}: {}",
                             deliveryLog.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Generate HMAC-SHA256 signature for webhook payload
     */
    private String generateHmacSignature(String payload, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Generate a timestamped signature header for replay protection.
     * Format: t=<seconds since epoch>,v1=<base64(HMAC(ts.payload))>
     */
    private String generateSignatureHeader(String payload, String secretKey, long timestampSeconds) {
        String canonical = timestampSeconds + "." + payload;
        String signature = generateHmacSignature(canonical, secretKey);
        return "t=" + timestampSeconds + ",v1=" + signature;
    }

    /**
     * Cleanup old delivery logs (older than 30 days)
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        log.info("Cleaning up webhook delivery logs older than {}", cutoffDate);

        try {
            deliveryLogRepository.deleteByCreatedAtBefore(cutoffDate);
        } catch (Exception e) {
            log.error("Error cleaning up old webhook logs: {}", e.getMessage());
        }
    }
}
