package com.ggumipooh.hanroroworld.be.util;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {
	private CookieUtil() {}

	public static void addHttpOnlyCookie(HttpServletResponse response,
									 String name,
									 String value,
									 int maxAgeSeconds,
									 String path,
									 boolean secure,
									 String sameSite) {
		addHttpOnlyCookie(response, name, value, maxAgeSeconds, path, secure, sameSite, null);
	}

	public static void addHttpOnlyCookie(HttpServletResponse response,
									 String name,
									 String value,
									 int maxAgeSeconds,
									 String path,
									 boolean secure,
									 String sameSite,
									 String domain) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value == null ? "" : value)
				.httpOnly(true)
				.secure(secure)
				.path(path == null ? "/" : path)
				.maxAge(maxAgeSeconds)
				.sameSite(sameSite);
		
		if (domain != null) {
			builder.domain(domain);
		}
		
		response.addHeader("Set-Cookie", builder.build().toString());
	}
} 


