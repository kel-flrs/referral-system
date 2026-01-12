package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.oauth.RestLoginResponse;
import com.bullhorn.mockservice.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API login endpoint matching Bullhorn's login flow
 */
@RestController
@RequestMapping("/rest-services")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "REST Login", description = "REST API session login (Bullhorn-compatible)")
public class RestLoginController {

    private final OAuthService oAuthService;

    /**
     * Step 3: REST API login using access token
     * POST /rest-services/login?version=*&access_token=XXX
     */
    @PostMapping("/login")
    @Operation(summary = "Login to REST API with access token",
               description = "Bullhorn OAuth Step 3: Exchange access token for REST session token (BhRestToken)")
    public ResponseEntity<RestLoginResponse> login(
            @RequestParam(defaultValue = "*") String version,
            @RequestParam("access_token") String accessToken) {

        log.info("REST login request with access token");

        RestLoginResponse response = oAuthService.loginWithAccessToken(accessToken, version);

        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint to invalidate session
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate session",
               description = "Invalidate the current REST session token")
    public ResponseEntity<Void> logout(@RequestParam("BhRestToken") String sessionToken) {
        log.info("Logout request for session");
        // TODO: Implement session invalidation
        return ResponseEntity.ok().build();
    }
}
