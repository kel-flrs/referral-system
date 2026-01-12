package com.bullhorn.mockservice.config;

import com.bullhorn.mockservice.service.OAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter to validate Bullhorn REST session tokens (BhRestToken)
 * Checks for token in query parameter, header, or cookie
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final OAuthService oAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Extract session token from request (query param, header, or cookie)
        String sessionToken = extractSessionToken(request);

        if (sessionToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Validate session token
            if (oAuthService.validateSessionToken(sessionToken)) {
                String username = oAuthService.getUsernameFromSession(sessionToken);

                if (username != null) {
                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Session validated for user: {}", username);
                }
            } else {
                log.debug("Invalid or expired session token");
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract session token from query parameter, header, or cookie
     * Matches Bullhorn's session token extraction logic
     */
    private String extractSessionToken(HttpServletRequest request) {
        // 1. Check query parameter
        String token = request.getParameter("BhRestToken");
        if (token != null) {
            return token;
        }

        // 2. Check header (BhRestToken or BHRestToken)
        token = request.getHeader("BhRestToken");
        if (token != null) {
            return token;
        }

        token = request.getHeader("BHRestToken");
        if (token != null) {
            return token;
        }

        // 3. Check cookie
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("BhRestToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
