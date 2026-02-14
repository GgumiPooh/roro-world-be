package com.ggumipooh.hanroroworld.be.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    @SuppressWarnings("unchecked")
    public static String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getDetails() instanceof Map)) {
            return null;
        }
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        return (String) details.get("name");
    }
}
