package com.bullhorn.mockservice.service.webhook;

import com.bullhorn.mockservice.dto.webhook.WebhookSubscriptionRequest;
import com.bullhorn.mockservice.dto.webhook.WebhookSubscriptionResponse;
import com.bullhorn.mockservice.entity.WebhookSubscription;
import com.bullhorn.mockservice.exception.DuplicateResourceException;
import com.bullhorn.mockservice.exception.ResourceNotFoundException;
import com.bullhorn.mockservice.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for managing webhook subscriptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public WebhookSubscriptionResponse createSubscription(WebhookSubscriptionRequest request) {
        log.info("Creating webhook subscription for {} to {}",
                request.getSubscriberEmail(), request.getCallbackUrl());

        // Check for duplicate
        subscriptionRepository.findBySubscriberEmailAndCallbackUrl(
            request.getSubscriberEmail(),
            request.getCallbackUrl()
        ).ifPresent(existing -> {
            throw new DuplicateResourceException(
                "Webhook subscription",
                "email+callbackUrl",
                request.getSubscriberEmail() + "+" + request.getCallbackUrl()
            );
        });

        // Generate secure secret key
        String secretKey = generateSecretKey();

        WebhookSubscription subscription = WebhookSubscription.builder()
            .subscriberName(request.getSubscriberName())
            .subscriberEmail(request.getSubscriberEmail())
            .callbackUrl(request.getCallbackUrl())
            .eventType(request.getEventType())
            .secretKey(secretKey)
            .active(true)
            .description(request.getDescription())
            .maxEventsPerMinute(request.getMaxEventsPerMinute())
            .maxRetryAttempts(request.getMaxRetryAttempts())
            .timeoutSeconds(request.getTimeoutSeconds())
            .build();

        WebhookSubscription saved = subscriptionRepository.save(subscription);

        log.info("Created webhook subscription with ID: {}", saved.getId());

        return toResponse(saved, true); // Include secret key on creation
    }

    @Transactional(readOnly = true)
    public WebhookSubscriptionResponse getSubscription(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription", "id", id));

        return toResponse(subscription, false); // Don't include secret key
    }

    @Transactional(readOnly = true)
    public Page<WebhookSubscriptionResponse> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable)
            .map(subscription -> toResponse(subscription, false));
    }

    @Transactional(readOnly = true)
    public Page<WebhookSubscriptionResponse> getSubscriptionsByEmail(String email, Pageable pageable) {
        return subscriptionRepository.findBySubscriberEmail(email)
            .stream()
            .map(subscription -> toResponse(subscription, false))
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
            ));
    }

    @Transactional
    public WebhookSubscriptionResponse toggleSubscription(Long id, boolean active) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription", "id", id));

        subscription.setActive(active);
        WebhookSubscription updated = subscriptionRepository.save(subscription);

        log.info("Toggled webhook subscription {} to active={}", id, active);

        return toResponse(updated, false);
    }

    @Transactional
    public void deleteSubscription(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription", "id", id));

        subscriptionRepository.delete(subscription);

        log.info("Deleted webhook subscription with ID: {}", id);
    }

    @Transactional
    public WebhookSubscriptionResponse regenerateSecret(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription", "id", id));

        String newSecretKey = generateSecretKey();
        subscription.setSecretKey(newSecretKey);

        WebhookSubscription updated = subscriptionRepository.save(subscription);

        log.info("Regenerated secret key for webhook subscription: {}", id);

        return toResponse(updated, true); // Include new secret key
    }

    /**
     * Generate a secure random secret key for HMAC signatures
     */
    private String generateSecretKey() {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Convert entity to response DTO
     */
    private WebhookSubscriptionResponse toResponse(WebhookSubscription subscription, boolean includeSecret) {
        return WebhookSubscriptionResponse.builder()
            .id(subscription.getId())
            .subscriberName(subscription.getSubscriberName())
            .subscriberEmail(subscription.getSubscriberEmail())
            .callbackUrl(subscription.getCallbackUrl())
            .eventType(subscription.getEventType())
            .secretKey(includeSecret ? subscription.getSecretKey() : "***HIDDEN***")
            .active(subscription.getActive())
            .description(subscription.getDescription())
            .maxEventsPerMinute(subscription.getMaxEventsPerMinute())
            .maxRetryAttempts(subscription.getMaxRetryAttempts())
            .timeoutSeconds(subscription.getTimeoutSeconds())
            .totalDeliveries(subscription.getTotalDeliveries())
            .failedDeliveries(subscription.getFailedDeliveries())
            .lastDeliveryAt(subscription.getLastDeliveryAt())
            .lastFailureAt(subscription.getLastFailureAt())
            .createdAt(subscription.getCreatedAt())
            .updatedAt(subscription.getUpdatedAt())
            .build();
    }
}
