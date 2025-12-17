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
		ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
				.httpOnly(true)
				.secure(secure)
				.path(path == null ? "/" : path)
				.maxAge(maxAgeSeconds)
				.sameSite(sameSite)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}
} 


