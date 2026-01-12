package com.bullhorn.mockservice.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility for creating and validating OAuth tokens
 */
@Component
public class JwtUtil {

    @Value("${app.oauth.jwt-secret:bullhorn-mock-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${app.oauth.issuer:bullhorn-mock-service}")
    private String issuer;

    /**
     * Generate access token (short-lived)
     */
    public String generateAccessToken(String clientId, String username, String scopes, int expirySeconds) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withClaim("client_id", clientId)
                .withClaim("scope", scopes)
                .withClaim("type", "access_token")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expirySeconds)))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    /**
     * Generate refresh token (long-lived)
     */
    public String generateRefreshToken(String clientId, String username, String scopes, int expirySeconds) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withClaim("client_id", clientId)
                .withClaim("scope", scopes)
                .withClaim("type", "refresh_token")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expirySeconds)))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    /**
     * Generate authorization code (very short-lived)
     */
    public String generateAuthorizationCode(String clientId, String username, String scopes) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withClaim("client_id", clientId)
                .withClaim("scope", scopes)
                .withClaim("type", "authorization_code")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(300))) // 5 minutes
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    /**
     * Verify and decode JWT token
     */
    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token);
    }

    /**
     * Extract client ID from token
     */
    public String extractClientId(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim("client_id").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract scopes from token
     */
    public String extractScopes(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim("scope").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
