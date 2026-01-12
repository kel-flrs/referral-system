package com.bullhorn.mockservice.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for OAuth token endpoint
 * Matches Bullhorn's token response format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;  // "Bearer"

    @JsonProperty("expires_in")
    private Integer expiresIn;  // Seconds until expiration

    @JsonProperty("refresh_token")
    private String refreshToken;  // Optional, for long-lived access

    private String scope;  // Granted scopes
}
