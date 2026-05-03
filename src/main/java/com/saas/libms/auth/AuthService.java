package com.saas.libms.auth;

import com.saas.libms.auth.blacklist.BlacklistedToken;
import com.saas.libms.auth.blacklist.BlacklistedTokenRepository;
import com.saas.libms.auth.dto.LoginRequest;
import com.saas.libms.auth.dto.LoginResponse;
import com.saas.libms.auth.dto.UserSummaryDto;
import com.saas.libms.auth.session.RefreshSession;
import com.saas.libms.auth.session.RefreshSessionRepository;
import com.saas.libms.exception.TokenException;
import com.saas.libms.exception.UnauthorizedException;
import com.saas.libms.institution.InstitutionStatus;
import com.saas.libms.security.CustomUserDetails;
import com.saas.libms.security.CustomUserDetailsService;
import com.saas.libms.security.JwtUtil;
import com.saas.libms.security.TokenHashUtil;
import com.saas.libms.user.User;
import com.saas.libms.user.UserRepository;
import com.saas.libms.user.UserStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshSessionRepository refreshSessionRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    //helpers
    private void setRefreshCookie(HttpServletResponse response,
                                  String value, int maxAge) {

        Cookie cookie = new Cookie("refresh_token", value);

        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        //SameSite =Strict
        response.addHeader("Set-Cookie",
                String.format("refresh_token=%s; Max-Age=%d; Path=/; SameSite=Strict",value,maxAge));

    }

    private String readRefreshCookie(HttpServletRequest request) {
        if(request.getCookies() == null) {
            throw new UnauthorizedException("No refresh token Found");
        }

        return Arrays.stream(request.getCookies())
                .filter(c-> "refresh_token".equals(c.getName()))
                .map((Cookie::getValue))
                .findFirst()
                .orElseThrow(()-> new UnauthorizedException("No refresh Token FouNd"));

    }

    @Transactional
    public LoginResponse login(LoginRequest request,
                               HttpServletRequest httpServletRequest,
                               HttpServletResponse httpServletResponse) {
        //Load user
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());

//        //Check PAssword
//        if(!passwordEncoder.matches(request.password(), userDetails.getPassword()) {
//            throw new UnauthorizedException("Invalid email or password");
//        }
//
//        //Check active user and institution
//        if(userDetails.getStatus() != UserStatus.ACTIVE) {
//            throw new UnauthorizedException("Your Account is disabled");
//        }
//
//        if(userDetails.getInstitutionStatus() != InstitutionStatus.ACTIVE) {
//            throw new UnauthorizedException("Your Instituion account is not active");
//        }

        //using Authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password())
        );

        //Only single Session
        refreshSessionRepository.deleteByUserId(userDetails.getUserId());

        //Generate Access token
        String accessToken = jwtUtil.generateAccessToken(
                userDetails.getUserId(),
                userDetails.getInstitutionId(),
                userDetails.getRole(),
                userDetails.getUsername());

        //Generate refresh token
        String refreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = TokenHashUtil.hash(refreshToken);
        User user = userDetails.getUser();


        RefreshSession session = RefreshSession.builder()
                .user(userDetails.getUser())
                .expiresAt(LocalDateTime.now().plusHours(10))
                .ipAddress(httpServletRequest.getRemoteAddr())
                .deviceInfo(httpServletRequest.getHeader("User-Agent"))
                .tokenHash(refreshTokenHash)
                .institutionId(user.getInstitution())
                .build();
        refreshSessionRepository.save(session);

        setRefreshCookie(httpServletResponse, refreshToken, 36000);

        UserSummaryDto userSummaryDto = new UserSummaryDto(
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getRole(),
                userDetails.getUser().getInstitution().getPublicId()
        );
        return LoginResponse.of(accessToken,userSummaryDto);

    }

    @Transactional
    public String refresh(HttpServletRequest httpServletRequest,
                          HttpServletResponse httpServletResponse) {
        //Read Refresh Token
        String rawRefreshToken = readRefreshCookie(httpServletRequest);

        //hash and check DB
        String hash = TokenHashUtil.hash(rawRefreshToken);
        RefreshSession session = refreshSessionRepository.findByTokenHash(hash)
                .orElseThrow(()-> new TokenException("Invalid or Expired refresh Token"));

        //Check session expiry
        if(session.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshSessionRepository.delete(session);
            throw new TokenException("Refresh token is expired. Please Log in Again");

        }
        //delete old session
        refreshSessionRepository.delete(session);

        //load user details
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(session.getUser().getEmail());

        //Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken( userDetails.getUserId(),
                userDetails.getInstitutionId(),
                userDetails.getRole(),
                userDetails.getUsername());

        //Generate new refresh token and save
        String newRefreshTokenHash = UUID.randomUUID().toString();
        RefreshSession newSession = RefreshSession.builder()
                .user(userDetails.getUser())
                .expiresAt(LocalDateTime.now().plusHours(10))
                .ipAddress(httpServletRequest.getRemoteAddr())
                .deviceInfo(httpServletRequest.getHeader("User-Agent"))
                .tokenHash(newRefreshTokenHash)
                .institutionId(userDetails.getUser().getInstitution())
                .build();
        refreshSessionRepository.save(session);
//set new Cookie
        setRefreshCookie(httpServletResponse, newRefreshTokenHash, 36000);

    return newAccessToken;

    }

    @Transactional
    public void logout(HttpServletResponse response,
                       HttpServletRequest request) {

        //Extract access Token
        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            String tokenHash = TokenHashUtil.hash(accessToken);

            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .tokenHash(tokenHash)
                    .expiresAt(jwtUtil.extractExpiry(accessToken))
                    .build();
            blacklistedTokenRepository.save(blacklistedToken);
        }

        //read refresh token and delete session
        try {
            String rawREfreshToken = readRefreshCookie(request);
            String hash = TokenHashUtil.hash(rawREfreshToken);
            refreshSessionRepository.findByTokenHash(hash)
                    .ifPresent(refreshSessionRepository::delete);
        } catch (UnauthorizedException e) {
            System.out.println(e.getMessage());
        }

        setRefreshCookie(response, "", 0);
    }

}
