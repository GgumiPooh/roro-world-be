package com.ggumipooh.hanroroworld.be.controller;

import lombok.RequiredArgsConstructor;
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
}
