package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;

@Service
public class TokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final SecretKey signingKey;
	private final int accessTtlSeconds;
	private final int refreshTtlSeconds;

	public TokenService(
			RefreshTokenRepository refreshTokenRepository,
			UserRepository userRepository,
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${app.jwt.accessTokenTtlSeconds:3600}") int accessTtlSeconds,
			@Value("${app.jwt.refreshTokenTtlSeconds:2592000}") int refreshTtlSeconds) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		this.accessTtlSeconds = accessTtlSeconds;
		this.refreshTtlSeconds = refreshTtlSeconds;
	}

	public record TokenPair(String accessToken, String refreshToken, Instant accessExpiresAt,
			Instant refreshExpiresAt) {
	}

	public TokenPair issueTokens(User user) {
		Instant now = Instant.now();
		Instant accessExp = now.plusSeconds(accessTtlSeconds);
		Instant refreshExp = now.plusSeconds(refreshTtlSeconds);

		String displayName = user.getNickname() != null ? user.getNickname() : user.getName();

		String accessJwt = Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("name", displayName == null ? "" : displayName)
				.issuedAt(Date.from(now))
				.expiration(Date.from(accessExp))
				.signWith(signingKey)
				.compact();

		String refreshPlain = generateSecureRandomToken();
		String refreshHash = sha256Hex(refreshPlain);
		RefreshToken entity = RefreshToken.builder()
				.userId(user.getId())
				.tokenHash(refreshHash)
				.expiresAt(refreshExp)
				.revoked(false)
				.build();
		refreshTokenRepository.save(entity);

		return new TokenPair(accessJwt, refreshPlain, accessExp, refreshExp);
	}

	public TokenPair rotateRefreshToken(String refreshTokenPlain, User user) {
		String hash = sha256Hex(refreshTokenPlain);
		RefreshToken existing = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
		if (existing.getExpiresAt().isBefore(Instant.now())) {
			throw new IllegalArgumentException("Refresh token expired");
		}
		existing.setRevoked(true);
		refreshTokenRepository.save(existing);
		return issueTokens(user);
	}

	public String hashRefreshToken(String refreshTokenPlain) {
		return sha256Hex(refreshTokenPlain);
	}

	/**
	 * JWT 서명 및 만료를 검증하고 사용자 ID(Long)를 반환합니다.
	 */
	public Long verifyAndExtractUserId(String jwt) {
		Claims claims = parseAndVerify(jwt);
		try {
			return Long.parseLong(claims.getSubject());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid sub");
		}
	}

	public User verifyAndExtractUser(String jwt) {
		Long userId = verifyAndExtractUserId(jwt);
		return userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	/**
	 * JWT에서 사용자 정보를 직접 추출 (DB 쿼리 없음)
	 */
	public Map<String, Object> verifyAndExtractClaims(String jwt) {
		Claims claims = parseAndVerify(jwt);
		String sub = claims.getSubject();
		String name = claims.get("name", String.class);

		return Map.of(
				"id", sub != null ? Long.parseLong(sub) : 0L,
				"name", name != null ? name : "");
	}

	private Claims parseAndVerify(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			throw new IllegalArgumentException("Missing token");
		}
		try {
			return Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(jwt)
					.getPayload();
		} catch (JwtException e) {
			throw new IllegalArgumentException("Invalid or expired token", e);
		}
	}

	private static String generateSecureRandomToken() {
		byte[] buf = new byte[32];
		new SecureRandom().nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
