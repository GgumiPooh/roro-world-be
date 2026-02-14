package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.NicknameRequest;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import java.time.Instant;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import org.springframework.transaction.annotation.Transactional;
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

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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
            var user = userRepository.findById(java.util.Objects.requireNonNull(existing.getUserId())).orElse(null);
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
    public Object currentUserName(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = readCookie(request, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "no_access_token";
        }
        try {
            // JWT에서 직접 추출 (DB 쿼리 없음 → 빠름!)
            var claims = tokenService.verifyAndExtractClaims(accessToken);
            return java.util.Map.of(
                    "id", claims.get("id"),
                    "name", claims.get("name"));
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_or_expired_token";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_load_name";
        }
    }

    @Transactional
    @DeleteMapping("/delete")
    public Object deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = readCookie(request, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "no_access_token";
        }
        try {
            long userId = tokenService.verifyAndExtractUserId(accessToken);
            var user = userRepository.findById(Long.valueOf(userId)).orElse(null);
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
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_or_expired_token";
        } catch (Exception ex) {
            ex.printStackTrace();
            response.setStatus(500);
            return "failed_to_delete_account: " + ex.getMessage();
        }
    }

    @Transactional
    @PutMapping("/nickname")
    public Object updateNickname(
            @RequestBody NicknameRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "no_access_token";
        }
        try {
            long userId = tokenService.verifyAndExtractUserId(accessToken);
            var user = userRepository.findById(Long.valueOf(userId)).orElse(null);
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
                return java.util.Map.of("nickname", user.getNickname());
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
                    (int) (pair.accessExpiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond()),
                    "/", isSecure, sameSite, null);

            return java.util.Map.of("nickname", user.getNickname());
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_or_expired_token";
        } catch (Exception ex) {
            ex.printStackTrace();
            response.setStatus(500);
            return "failed_to_update_nickname: " + ex.getMessage();
        }
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()))
                return c.getValue();
        }
        return null;
    }
}
