package com.bullhorn.mockservice.dto.webhook;

import com.bullhorn.mockservice.webhook.WebhookEventType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a webhook subscription
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionRequest {

    @NotBlank(message = "Subscriber name is required")
    @Size(min = 3, max = 100, message = "Subscriber name must be between 3 and 100 characters")
    private String subscriberName;

    @NotBlank(message = "Subscriber email is required")
    @Email(message = "Invalid email format")
    private String subscriberEmail;

    @NotBlank(message = "Callback URL is required")
    // Allow http/https for local testing; tighten to https in production
    @Pattern(regexp = "^https?://.*", message = "Callback URL must use http/https")
    @Size(max = 500, message = "Callback URL too long")
    private String callbackUrl;

    @NotNull(message = "Event type is required")
    private WebhookEventType eventType;

    @Size(max = 500, message = "Description too long")
    private String description;

    @Builder.Default
    @Min(value = 1, message = "Max events per minute must be at least 1")
    @Max(value = 1000, message = "Max events per minute cannot exceed 1000")
    private Integer maxEventsPerMinute = 60;

    @Builder.Default
    @Min(value = 1, message = "Max retry attempts must be at least 1")
    @Max(value = 10, message = "Max retry attempts cannot exceed 10")
    private Integer maxRetryAttempts = 5;

    @Builder.Default
    @Min(value = 5, message = "Timeout must be at least 5 seconds")
    @Max(value = 60, message = "Timeout cannot exceed 60 seconds")
    private Integer timeoutSeconds = 30;
}
