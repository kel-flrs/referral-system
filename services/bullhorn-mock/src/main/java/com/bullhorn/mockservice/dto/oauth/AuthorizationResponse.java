package com.bullhorn.mockservice.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for OAuth authorization endpoint
 * Returns authorization code to be exchanged for access token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationResponse {
    private String code;          // Authorization code
    private String state;         // Client state for CSRF protection
}
