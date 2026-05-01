package com.saas.libms.security;

import com.saas.libms.auth.blacklist.BlacklistedTokenRepository;
import com.saas.libms.exception.TokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once on every HTTP request before it hits a controller.
 *
 * What it does:
 *  1. Reads the JWT from the "Authorization: Bearer <token>" header
 *  2. Validates the signature and expiry
 *  3. Checks the token is NOT in the blacklist (wasn't logged out)
 *  4. Loads the user and puts them in the SecurityContext
 *
 * If anything fails, the request continues as anonymous —
 * Spring Security then rejects it for protected endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        // No token = anonymous request — let Spring Security handle it downstream
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 1 — Is the JWT itself valid (signature + expiry)?
        if (!jwtUtil.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2 — Was this token blacklisted (user logged out but token hasn't expired yet)?
        String tokenHash = TokenHashUtil.hash(token);
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            log.warn("Rejected blacklisted token for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3 — Load the user and authenticate them in the SecurityContext
        String email = jwtUtil.extractUserId(token).toString();

        // Only set authentication if not already set (prevents processing twice)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateUser(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            // We stored userId as the subject — look up by userId via a custom method
            // For simplicity here we load by the email extracted from claims
            // In your UserRepository, you'll add findById too
            String email = extractEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                          // credentials — null after login
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (TokenException ex) {
            log.error("Failed to authenticate user from JWT: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Reads the Bearer token from the Authorization header.
     * Returns null if the header is missing or not a Bearer token.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // strip "Bearer " prefix
        }
        return null;
    }

    /**
     * We stored userId as the JWT subject, but to load UserDetails
     * we need email. We add an email claim to the token in JwtUtil.
     *
     * NOTE: Update JwtUtil.generateAccessToken() to also embed email
     * as a claim, then extract it here.
     */
    private String extractEmailFromToken(String token) {
        // This will work once you add "email" as a claim in JwtUtil.generateAccessToken()
        // For now, this delegates to a method we'll wire up in Phase 5 (AuthService/JwtUtil update)
        return jwtUtil.extractEmail(token);
    }
}
