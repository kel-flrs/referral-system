package com.bullhorn.mockservice.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.bullhorn.mockservice.dto.oauth.AuthorizationResponse;
import com.bullhorn.mockservice.dto.oauth.RestLoginResponse;
import com.bullhorn.mockservice.dto.oauth.TokenResponse;
import com.bullhorn.mockservice.entity.OAuthClient;
import com.bullhorn.mockservice.entity.RefreshToken;
import com.bullhorn.mockservice.entity.RestSession;
import com.bullhorn.mockservice.exception.BullhornApiException;
import com.bullhorn.mockservice.repository.OAuthClientRepository;
import com.bullhorn.mockservice.repository.RefreshTokenRepository;
import com.bullhorn.mockservice.repository.RestSessionRepository;
import com.bullhorn.mockservice.service.OAuthService;
import com.bullhorn.mockservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    private final OAuthClientRepository oAuthClientRepository;
    private final RestSessionRepository restSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth.session-validity-seconds:3600}")
    private Integer sessionValiditySeconds;

    @Value("${server.port:8080}")
    private String serverPort;

    @Override
    @Transactional
    public AuthorizationResponse authorize(String clientId, String username, String password,
                                          String responseType, String state) {
        log.info("Authorization request for client: {}, user: {}", clientId, username);

        // Validate response type
        if (!"code".equals(responseType)) {
            throw new BullhornApiException("Unsupported response_type. Only 'code' is supported.");
        }

        // Find and validate client
        OAuthClient client = oAuthClientRepository.findByClientIdAndEnabled(clientId, true)
                .orElseThrow(() -> new BullhornApiException("Invalid client_id or client is disabled"));

        // Validate username matches
        if (!client.getUsername().equals(username)) {
            throw new BullhornApiException("Invalid username for this client");
        }

        // Validate password
        if (!passwordEncoder.matches(password, client.getPassword())) {
            throw new BullhornApiException("Invalid password");
        }

        // Generate authorization code (short-lived JWT)
        String authCode = jwtUtil.generateAuthorizationCode(clientId, username, client.getScopes());

        log.info("Authorization code generated for client: {}", clientId);

        return AuthorizationResponse.builder()
                .code(authCode)
                .state(state)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse exchangeCodeForToken(String grantType, String code, String clientId, String clientSecret) {
        log.info("Token exchange request for client: {}", clientId);

        // Validate grant type
        if (!"authorization_code".equals(grantType)) {
            throw new BullhornApiException("Unsupported grant_type");
        }

        // Validate client credentials
        OAuthClient client = oAuthClientRepository.findByClientIdAndEnabled(clientId, true)
                .orElseThrow(() -> new BullhornApiException("Invalid client credentials"));

        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new BullhornApiException("Invalid client secret");
        }

        // Verify and decode authorization code
        DecodedJWT decodedCode;
        try {
            decodedCode = jwtUtil.verifyToken(code);

            // Verify it's an authorization code
            if (!"authorization_code".equals(decodedCode.getClaim("type").asString())) {
                throw new BullhornApiException("Invalid authorization code");
            }

            // Verify client ID matches
            if (!clientId.equals(decodedCode.getClaim("client_id").asString())) {
                throw new BullhornApiException("Client ID mismatch");
            }
        } catch (Exception e) {
            throw new BullhornApiException("Invalid or expired authorization code: " + e.getMessage());
        }

        String username = decodedCode.getSubject();
        String scopes = decodedCode.getClaim("scope").asString();

        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(
                clientId,
                username,
                scopes,
                client.getAccessTokenValiditySeconds()
        );

        // Generate refresh token
        String refreshTokenValue = jwtUtil.generateRefreshToken(
                clientId,
                username,
                scopes,
                client.getRefreshTokenValiditySeconds()
        );

        // Store refresh token in database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .clientId(clientId)
                .username(username)
                .scopes(scopes)
                .expiresAt(LocalDateTime.now().plusSeconds(client.getRefreshTokenValiditySeconds()))
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Access token and refresh token generated for client: {}", clientId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(client.getAccessTokenValiditySeconds())
                .refreshToken(refreshTokenValue)
                .scope(scopes)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshAccessToken(String refreshTokenValue, String clientId, String clientSecret) {
        log.info("Refresh token request for client: {}", clientId);

        // Validate client credentials
        OAuthClient client = oAuthClientRepository.findByClientIdAndEnabled(clientId, true)
                .orElseThrow(() -> new BullhornApiException("Invalid client credentials"));

        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new BullhornApiException("Invalid client secret");
        }

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new BullhornApiException("Invalid or revoked refresh token"));

        // Verify not expired
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BullhornApiException("Refresh token has expired");
        }

        // Verify client ID matches
        if (!clientId.equals(refreshToken.getClientId())) {
            throw new BullhornApiException("Client ID mismatch");
        }

        // Generate new access token
        String accessToken = jwtUtil.generateAccessToken(
                clientId,
                refreshToken.getUsername(),
                refreshToken.getScopes(),
                client.getAccessTokenValiditySeconds()
        );

        log.info("New access token generated from refresh token for client: {}", clientId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(client.getAccessTokenValiditySeconds())
                .refreshToken(refreshTokenValue) // Return same refresh token
                .scope(refreshToken.getScopes())
                .build();
    }

    @Override
    @Transactional
    public RestLoginResponse loginWithAccessToken(String accessToken, String version) {
        log.info("REST login request");

        // Verify access token
        DecodedJWT decodedToken;
        try {
            decodedToken = jwtUtil.verifyToken(accessToken);

            // Verify it's an access token
            if (!"access_token".equals(decodedToken.getClaim("type").asString())) {
                throw new BullhornApiException("Invalid token type. Expected access_token.");
            }
        } catch (Exception e) {
            throw new BullhornApiException("Invalid or expired access token: " + e.getMessage());
        }

        String clientId = decodedToken.getClaim("client_id").asString();
        String username = decodedToken.getSubject();

        // Generate session token
        String sessionToken = UUID.randomUUID().toString().replace("-", "");

        // Generate REST URL
        String restUrl = String.format("http://localhost:%s/rest-services/", serverPort);

        // Create and save session
        RestSession session = RestSession.builder()
                .sessionToken(sessionToken)
                .restUrl(restUrl)
                .clientId(clientId)
                .username(username)
                .expiresAt(LocalDateTime.now().plusSeconds(sessionValiditySeconds))
                .active(true)
                .build();
        restSessionRepository.save(session);

        log.info("REST session created for user: {}, client: {}", username, clientId);

        return RestLoginResponse.builder()
                .BhRestToken(sessionToken)
                .restUrl(restUrl)
                .sessionExpires(sessionValiditySeconds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateSessionToken(String sessionToken) {
        return restSessionRepository.findBySessionTokenAndActiveTrue(sessionToken)
                .map(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public String getUsernameFromSession(String sessionToken) {
        return restSessionRepository.findBySessionTokenAndActiveTrue(sessionToken)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(RestSession::getUsername)
                .orElse(null);
    }
}
