package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import java.time.Instant;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import com.ggumipooh.hanroroworld.be.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    @GetMapping("/sign-in")
    public String signIn() {
        return "Hello World";
    }

    @GetMapping("/sign-up")
    public String signUp() {
        return "Hello World";
    }

    @GetMapping("/me")
    public Object me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return "anonymous";
        }
        return user.getAttributes();
    }

    @PostMapping("/refresh")
    public Object refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readCookie(request, "refresh_token");
        if (refreshToken == null) {
            response.setStatus(401);
            return "no_refresh_token";
        }
        try {
            String hash = tokenService.hashRefreshToken(refreshToken);
            RefreshToken existing = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                    .orElse(null);
            if (existing == null || existing.getExpiresAt().isBefore(Instant.now())) {
                response.setStatus(401);
                return "invalid_or_expired_refresh";
            }
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            var user = userRepository.findById(existing.getUserId()).orElse(null);
            if (user == null) {
                response.setStatus(401);
                return "user_not_found";
            }
            var pair = tokenService.issueTokens(user);
            CookieUtil.addHttpOnlyCookie(response, "access_token", pair.accessToken(), (int) (pair.accessExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()), "/", true, "Lax");
            CookieUtil.addHttpOnlyCookie(response, "refresh_token", pair.refreshToken(), (int) (pair.refreshExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()), "/api/auth", true, "Strict");
            return "ok";
        } catch (Exception e) {
            response.setStatus(500);
            return "refresh_failed";
        }
    }

    @PostMapping("/logout")
    public Object logout(HttpServletResponse response) {
        CookieUtil.addHttpOnlyCookie(response, "access_token", "", 0, "/", true, "Lax");
        CookieUtil.addHttpOnlyCookie(response, "refresh_token", "", 0, "/api/auth", true, "Strict");
        return "ok";
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
