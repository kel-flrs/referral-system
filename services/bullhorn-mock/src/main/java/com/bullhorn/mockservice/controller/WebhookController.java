package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.response.ApiResponse;
import com.bullhorn.mockservice.dto.webhook.WebhookSubscriptionRequest;
import com.bullhorn.mockservice.dto.webhook.WebhookSubscriptionResponse;
import com.bullhorn.mockservice.dto.webhook.WebhookTestRequest;
import com.bullhorn.mockservice.service.webhook.WebhookService;
import com.bullhorn.mockservice.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * REST Controller for Webhook management
 * Allows consumers to subscribe to real-time events
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION + "/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook subscription and management APIs")
public class WebhookController {

    private final WebhookService webhookService;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/subscriptions")
    @Operation(summary = "Subscribe to webhook events",
               description = "Register a callback URL to receive real-time notifications when data changes")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> createSubscription(
            @Valid @RequestBody WebhookSubscriptionRequest request
    ) {
        log.info("Creating webhook subscription for {}", request.getSubscriberEmail());

        WebhookSubscriptionResponse subscription = webhookService.createSubscription(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(subscription,
                "Webhook subscription created successfully. Save the secret key securely!"));
    }

    @GetMapping("/subscriptions/{id}")
    @Operation(summary = "Get webhook subscription by ID")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> getSubscription(
            @PathVariable Long id
    ) {
        WebhookSubscriptionResponse subscription = webhookService.getSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "List all webhook subscriptions")
    public ResponseEntity<ApiResponse<Page<WebhookSubscriptionResponse>>> listSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<WebhookSubscriptionResponse> subscriptions = webhookService.getAllSubscriptions(pageable);

        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }

    @GetMapping("/subscriptions/by-email")
    @Operation(summary = "Get subscriptions by subscriber email")
    public ResponseEntity<ApiResponse<Page<WebhookSubscriptionResponse>>> getSubscriptionsByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<WebhookSubscriptionResponse> subscriptions =
            webhookService.getSubscriptionsByEmail(email, pageable);

        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }

    @PatchMapping("/subscriptions/{id}/toggle")
    @Operation(summary = "Enable or disable a webhook subscription")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> toggleSubscription(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        log.info("Toggling webhook subscription {} to active={}", id, active);

        WebhookSubscriptionResponse subscription = webhookService.toggleSubscription(id, active);

        return ResponseEntity.ok(ApiResponse.success(subscription,
            active ? "Subscription enabled" : "Subscription disabled"));
    }

    @PostMapping("/subscriptions/{id}/regenerate-secret")
    @Operation(summary = "Regenerate secret key for a subscription")
    public ResponseEntity<ApiResponse<WebhookSubscriptionResponse>> regenerateSecret(
            @PathVariable Long id
    ) {
        log.info("Regenerating secret key for subscription {}", id);

        WebhookSubscriptionResponse subscription = webhookService.regenerateSecret(id);

        return ResponseEntity.ok(ApiResponse.success(subscription,
            "Secret key regenerated successfully. Save it securely!"));
    }

    @DeleteMapping("/subscriptions/{id}")
    @Operation(summary = "Delete a webhook subscription")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(
            @PathVariable Long id
    ) {
        log.info("Deleting webhook subscription {}", id);

        webhookService.deleteSubscription(id);

        return ResponseEntity.ok(ApiResponse.success(null,
            "Webhook subscription deleted successfully"));
    }

    @PostMapping("/test")
    @Operation(summary = "Test a webhook endpoint",
               description = "Send a test webhook to verify your endpoint is working correctly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testWebhook(
            @Valid @RequestBody WebhookTestRequest request
    ) {
        log.info("Testing webhook endpoint: {}", request.getCallbackUrl());

        try {
            // Create test payload
            Map<String, Object> testPayload = new HashMap<>();
            testPayload.put("eventType", "test.ping");
            testPayload.put("timestamp", LocalDateTime.now());
            testPayload.put("message", "This is a test webhook from Bullhorn Mock Service");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Event-Type", "test.ping");
            headers.set("User-Agent", "Bullhorn-Mock-Service-Webhook/1.0");

            String secret = request.getSignatureSecret() == null || request.getSignatureSecret().isEmpty()
                ? "test-secret"
                : request.getSignatureSecret();

            String payloadJson = objectMapper.writeValueAsString(testPayload);

            long timestampSeconds = Instant.now().getEpochSecond();
            String v2Signature = "t=" + timestampSeconds + ",v1=" + hmacSha256(timestampSeconds + "." + payloadJson, secret);

            headers.set("X-Webhook-Signature", v2Signature);
            headers.set("X-Webhook-Timestamp", String.valueOf(timestampSeconds));
            headers.set("X-Webhook-Test-Secret", secret); // helps consumers validate locally

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(testPayload, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = webhookRestTemplate.postForEntity(
                request.getCallbackUrl(),
                httpRequest,
                String.class
            );
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getStatusCode().is2xxSuccessful());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());
            result.put("durationMs", duration);
            result.put("signatureSecret", secret);
            result.put("signature", v2Signature);

            return ResponseEntity.ok(ApiResponse.success(result,
                "Webhook test completed successfully"));

        } catch (Exception e) {
            log.error("Webhook test failed for {}: {}", request.getCallbackUrl(), e.getMessage());

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Webhook test failed: " + e.getMessage())
                    .data(result)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    private String hmacSha256(String payload, String secretKey) {
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
}
