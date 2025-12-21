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
			@Value("${app.jwt.accessTokenTtlSeconds:3600}") int accessTtlSeconds,
			@Value("${app.jwt.refreshTokenTtlSeconds:2592000}") int refreshTtlSeconds) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
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

		String accessJwt = createJwt(Map.of(
				"sub", String.valueOf(user.getId()),
				"nickname", user.getNickname() == null ? "" : user.getNickname(),
				"iat", now.getEpochSecond(),
				"exp", accessExp.getEpochSecond()));

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

	/**
	 * Verifies the access JWT signature and expiration, then returns the subject
	 * (user id) as Long.
	 * Throws IllegalArgumentException if invalid.
	 */
	public Long verifyAndExtractUserId(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			throw new IllegalArgumentException("Missing token");
		}
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid token format");
		}
		String header = parts[0];
		String payload = parts[1];
		String signature = parts[2];

		// Verify signature
		String expectedSig = hmacSha256(header + "." + payload, jwtSecret);
		if (!constantTimeEquals(signature, expectedSig)) {
			throw new IllegalArgumentException("Invalid token signature");
		}

		// Decode payload
		String payloadJson = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);

		// Extract exp and sub with lightweight parsing (no external JSON parser)
		Long exp = extractLongValue(payloadJson, "\"exp\"");
		if (exp == null) {
			throw new IllegalArgumentException("Missing exp");
		}
		if (Instant.now().getEpochSecond() >= exp) {
			throw new IllegalArgumentException("Token expired");
		}
		// sub can be quoted or number
		String subStr = extractStringOrNumberValue(payloadJson, "\"sub\"");
		if (subStr == null) {
			throw new IllegalArgumentException("Missing sub");
		}
		try {
			return Long.parseLong(subStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid sub");
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null)
			return false;
		if (a.length() != b.length())
			return false;
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}
		return result == 0;
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

	private static byte[] base64UrlDecode(String value) {
		return Base64.getUrlDecoder().decode(value);
	}

	/**
	 * Extracts a long value for a given JSON key (very small helper; not a full
	 * JSON parser).
	 * Accepts forms like: "key":123 or "key":"123"
	 */
	private static Long extractLongValue(String json, String keyWithQuotes) {
		String value = extractStringOrNumberValue(json, keyWithQuotes);
		if (value == null)
			return null;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Extracts a string of digits for a given key. If the JSON contains "key":"123"
	 * returns 123, if "key":123 returns 123.
	 * Returns null if not found.
	 */
	private static String extractStringOrNumberValue(String json, String keyWithQuotes) {
		int idx = json.indexOf(keyWithQuotes);
		if (idx < 0)
			return null;
		int colon = json.indexOf(':', idx + keyWithQuotes.length());
		if (colon < 0)
			return null;
		// Skip whitespace
		int i = colon + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i)))
			i++;
		if (i >= json.length())
			return null;
		char c = json.charAt(i);
		if (c == '\"') {
			// Read until next quote
			int end = json.indexOf('\"', i + 1);
			if (end < 0)
				return null;
			return json.substring(i + 1, end);
		} else {
			// Read number token
			int end = i;
			while (end < json.length()) {
				char ch = json.charAt(end);
				if ((ch >= '0' && ch <= '9')) {
					end++;
				} else {
					break;
				}
			}
			if (end == i)
				return null;
			return json.substring(i, end);
		}
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
				if (!first)
					sb.append(",");
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
