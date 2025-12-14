package com.ggumipooh.hanroroworld.be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
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
}
