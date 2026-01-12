package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.dto.oauth.AuthorizationResponse;
import com.bullhorn.mockservice.dto.oauth.RestLoginResponse;
import com.bullhorn.mockservice.dto.oauth.TokenResponse;

/**
 * OAuth service interface for Bullhorn-style OAuth 2.0 flow
 */
public interface OAuthService {

    /**
     * Step 1: Authorize client and return authorization code
     */
    AuthorizationResponse authorize(String clientId, String username, String password, String responseType, String state);

    /**
     * Step 2: Exchange authorization code for access token
     */
    TokenResponse exchangeCodeForToken(String grantType, String code, String clientId, String clientSecret);

    /**
     * Step 2b: Exchange refresh token for new access token
     */
    TokenResponse refreshAccessToken(String refreshToken, String clientId, String clientSecret);

    /**
     * Step 3: Login to REST API using access token
     */
    RestLoginResponse loginWithAccessToken(String accessToken, String version);

    /**
     * Validate session token
     */
    boolean validateSessionToken(String sessionToken);

    /**
     * Get username from session token
     */
    String getUsernameFromSession(String sessionToken);
}
