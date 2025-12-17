package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.RefreshToken;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final byte[] jwtSecret;
	private final int accessTtlSeconds;
	private final int refreshTtlSeconds;

	public TokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${app.jwt.secret:local-dev-secret-change-me}") String jwtSecret,
			@Value("${app.jwt.accessTokenTtlSeconds:900}") int accessTtlSeconds,
			@Value("${app.jwt.refreshTokenTtlSeconds:2592000}") int refreshTtlSeconds
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
		this.accessTtlSeconds = accessTtlSeconds;
		this.refreshTtlSeconds = refreshTtlSeconds;
	}

	public record TokenPair(String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {}

	public TokenPair issueTokens(User user) {
		Instant now = Instant.now();
		Instant accessExp = now.plusSeconds(accessTtlSeconds);
		Instant refreshExp = now.plusSeconds(refreshTtlSeconds);

		String accessJwt = createJwt(Map.of(
				"sub", String.valueOf(user.getId()),
				"nickname", user.getNickname() == null ? "" : user.getNickname(),
				"iat", now.getEpochSecond(),
				"exp", accessExp.getEpochSecond()
		));

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
		// revoke old
		existing.setRevoked(true);
		refreshTokenRepository.save(existing);
		// issue new pair
		return issueTokens(user);
	}
	
	public String hashRefreshToken(String refreshTokenPlain) {
		return sha256Hex(refreshTokenPlain);
	}

	private String createJwt(Map<String, Object> claims) {
		String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
		String payloadJson = JsonUtil.toJson(claims);
		String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
		String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
		String signingInput = header + "." + payload;
		String signature = hmacSha256(signingInput, jwtSecret);
		return signingInput + "." + signature;
	}

	private static String hmacSha256(String data, byte[] secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return base64UrlEncode(sig);
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign JWT", e);
		}
	}

	private static String base64UrlEncode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String generateSecureRandomToken() {
		byte[] buf = new byte[32];
		new SecureRandom().nextBytes(buf);
		return base64UrlEncode(buf);
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

	// Minimal JSON util to avoid extra deps
	static final class JsonUtil {
		static String toJson(Map<String, Object> map) {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			boolean first = true;
			for (Map.Entry<String, Object> e : map.entrySet()) {
				if (!first) sb.append(",");
				first = false;
				sb.append("\"").append(escape(e.getKey())).append("\":");
				Object v = e.getValue();
				if (v == null) {
					sb.append("null");
				} else if (v instanceof Number || v instanceof Boolean) {
					sb.append(v.toString());
				} else {
					sb.append("\"").append(escape(String.valueOf(v))).append("\"");
				}
			}
			sb.append("}");
			return sb.toString();
		}

		private static String escape(String s) {
			return s.replace("\\", "\\\\").replace("\"", "\\\"");
		}
	}
}


