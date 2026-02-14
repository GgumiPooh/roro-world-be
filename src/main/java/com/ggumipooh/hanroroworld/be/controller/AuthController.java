package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.NicknameRequest;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import com.ggumipooh.hanroroworld.be.security.SecurityUtil;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import com.ggumipooh.hanroroworld.be.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * refresh_token 쿠키로 새 토큰 쌍 발급.
     * refresh_token은 Path=/api/auth 전용이므로 직접 쿠키에서 읽어야 함.
     */
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
            var user = userRepository.findById(Objects.requireNonNull(existing.getUserId())).orElse(null);
            if (user == null) {
                response.setStatus(401);
                return "user_not_found";
            }
            var pair = tokenService.issueTokens(user);
            boolean isSecure = frontendUrl.startsWith("https");
            String sameSite = isSecure ? "None" : "Lax";
            CookieUtil.addHttpOnlyCookie(response, "access_token", pair.accessToken(),
                    (int) (pair.accessExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()), "/", isSecure,
                    sameSite, null);
            CookieUtil.addHttpOnlyCookie(response, "refresh_token", pair.refreshToken(),
                    (int) (pair.refreshExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()), "/api/auth",
                    isSecure, sameSite, null);
            return "ok";
        } catch (Exception e) {
            response.setStatus(500);
            return "refresh_failed";
        }
    }

    @PostMapping("/logout")
    public Object logout(HttpServletResponse response) {
        boolean isSecure = frontendUrl.startsWith("https");
        String sameSite = isSecure ? "None" : "Lax";
        CookieUtil.addHttpOnlyCookie(response, "access_token", "", 0, "/", isSecure, sameSite, null);
        CookieUtil.addHttpOnlyCookie(response, "refresh_token", "", 0, "/api/auth", isSecure, sameSite, null);
        return "ok";
    }

    @GetMapping("/name")
    public Object currentUserName(HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "no_access_token";
        }
        String name = SecurityUtil.getCurrentUserName();
        return Map.of("id", userId, "name", name != null ? name : "");
    }

    @Transactional
    @DeleteMapping("/delete")
    public Object deleteAccount(HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "no_access_token";
        }
        try {
            var user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                response.setStatus(401);
                return "user_not_found";
            }

            // Hard delete - 개인정보 완전 삭제
            userRepository.delete(user);

            // 쿠키 삭제 (로그아웃 처리)
            boolean isSecure = frontendUrl.startsWith("https");
            String sameSite = isSecure ? "None" : "Lax";
            CookieUtil.addHttpOnlyCookie(response, "access_token", "", 0, "/", isSecure, sameSite, null);
            CookieUtil.addHttpOnlyCookie(response, "refresh_token", "", 0, "/api/auth", isSecure, sameSite, null);

            return "ok";
        } catch (Exception ex) {
            log.error("Failed to delete account", ex);
            response.setStatus(500);
            return "failed_to_delete_account";
        }
    }

    @Transactional
    @PutMapping("/nickname")
    public Object updateNickname(
            @RequestBody NicknameRequest request,
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "no_access_token";
        }
        try {
            var user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                response.setStatus(401);
                return "user_not_found";
            }

            String newNickname = request.getNickname();
            if (newNickname == null || newNickname.trim().length() < 2) {
                response.setStatus(400);
                return "nickname_too_short";
            }

            String trimmedNickname = newNickname.trim();

            // 현재 닉네임과 같으면 변경 없이 성공
            if (trimmedNickname.equals(user.getNickname())) {
                return Map.of("nickname", user.getNickname());
            }

            // 중복 체크
            if (userRepository.existsByNickname(trimmedNickname)) {
                response.setStatus(409);
                return "nickname_already_exists";
            }

            user.setNickname(trimmedNickname);
            userRepository.save(user);

            // 토큰 재발급 (새 닉네임 반영)
            var pair = tokenService.issueTokens(user);
            boolean isSecure = frontendUrl.startsWith("https");
            String sameSite = isSecure ? "None" : "Lax";
            CookieUtil.addHttpOnlyCookie(response, "access_token", pair.accessToken(),
                    (int) (pair.accessExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()),
                    "/", isSecure, sameSite, null);

            return Map.of("nickname", user.getNickname());
        } catch (Exception ex) {
            log.error("Failed to update nickname", ex);
            response.setStatus(500);
            return "failed_to_update_nickname";
        }
    }

    /**
     * refresh_token 쿠키 전용 헬퍼.
     * access_token은 JwtAuthenticationFilter → SecurityContext로 처리됨.
     */
    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
