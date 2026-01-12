package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.oauth.AuthorizationResponse;
import com.bullhorn.mockservice.dto.oauth.TokenResponse;
import com.bullhorn.mockservice.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 2.0 endpoints matching Bullhorn's OAuth flow
 */
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth", description = "OAuth 2.0 authorization endpoints (Bullhorn-compatible)")
public class OAuthController {

    private final OAuthService oAuthService;

    /**
     * Step 1: Authorization endpoint
     * GET /oauth/authorize?client_id=XXX&response_type=code&username=XXX&password=XXX&action=Login
     */
    @GetMapping("/authorize")
    @Operation(summary = "Get authorization code",
               description = "Bullhorn OAuth Step 1: Authorize user and get authorization code")
    public ResponseEntity<AuthorizationResponse> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(name = "response_type", defaultValue = "code") String responseType,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "Login") String action) {

        log.info("Authorization request - client_id: {}, username: {}", clientId, username);

        AuthorizationResponse response = oAuthService.authorize(clientId, username, password, responseType, state);

        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Token endpoint
     * POST /oauth/token
     * Form data: grant_type=authorization_code&code=XXX&client_id=XXX&client_secret=XXX
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Exchange authorization code for access token",
               description = "Bullhorn OAuth Step 2: Exchange authorization code for access token and refresh token")
    public ResponseEntity<TokenResponse> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(required = false) String code,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam(name = "refresh_token", required = false) String refreshToken) {

        log.info("Token request - grant_type: {}, client_id: {}", grantType, clientId);

        TokenResponse response;

        if ("authorization_code".equals(grantType)) {
            response = oAuthService.exchangeCodeForToken(grantType, code, clientId, clientSecret);
        } else if ("refresh_token".equals(grantType)) {
            response = oAuthService.refreshAccessToken(refreshToken, clientId, clientSecret);
        } else {
            throw new IllegalArgumentException("Unsupported grant_type: " + grantType);
        }

        return ResponseEntity.ok(response);
    }
}
