package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.security.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                }) // ✅ Security에서 CORS 활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/oauth2/**", "/oauth2.0/**", "/login/**", "/api/auth/**",
                                "/api/public/auth/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae.baseUri("/oauth2.0/authorization"))
                        .redirectionEndpoint(redir -> redir.baseUri("/api/public/auth/*/callback"))
                        .userInfoEndpoint(cfg -> cfg.userService(customOAuth2UserService)));

        return http.build();
    }
}
