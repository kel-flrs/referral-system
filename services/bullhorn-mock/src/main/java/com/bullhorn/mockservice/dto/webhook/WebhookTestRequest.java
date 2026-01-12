package com.bullhorn.mockservice.dto.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for testing a webhook endpoint before subscribing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookTestRequest {

    @NotBlank(message = "Callback URL is required")
    @Pattern(regexp = "^https://.*", message = "Callback URL must use HTTPS")
    private String callbackUrl;

    // Optional secret to sign the test webhook with; defaults to "test-secret" if omitted.
    private String signatureSecret;
}
