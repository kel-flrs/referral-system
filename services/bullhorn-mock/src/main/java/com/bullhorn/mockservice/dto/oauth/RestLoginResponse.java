package com.bullhorn.mockservice.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for REST API login endpoint
 * Matches Bullhorn's login response format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestLoginResponse {

    private String BhRestToken;     // Session token for API calls
    private String restUrl;         // Base URL for REST API calls
    private Integer sessionExpires; // Session expiry in seconds
}
