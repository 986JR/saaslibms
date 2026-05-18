package com.saas.libms.security;

import com.saas.libms.auth.blacklist.BlacklistedTokenRepository;
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

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromHeader(request);

        // 1. No token → anonymous request
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Invalid token → 401
        if (!jwtUtil.isValid(token)) {
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        // 3. Blacklisted token → 401
        String tokenHash = TokenHashUtil.hash(token);
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            writeUnauthorized(response, "Token has been revoked");
            return;
        }

        // 4. Authenticate user (Step 3 + Step 4 clean handling)
        try {

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(token, request);
            }

        } catch (Exception ex) {

            log.error("JWT authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();

            writeUnauthorized(response, "Authentication failed");
            return;
        }

        // 5. Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * STEP 3 CORE LOGIC (clean + safe)
     */
    private void authenticateUser(String token, HttpServletRequest request) {

        String email = extractEmailFromToken(token);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Clean reusable 401 response helper (STEP 4 style)
     */
    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write("""
        {
            "status": 401,
            "error": "%s"
        }
        """.formatted(message));
    }

    /**
     * Extract Bearer token
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    /**
     * Extract email from JWT claims
     */
    private String extractEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }
}