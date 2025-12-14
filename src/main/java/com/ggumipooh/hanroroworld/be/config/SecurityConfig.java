package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.security.NaverOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final NaverOAuth2UserService naverOAuth2UserService;

    public SecurityConfig(NaverOAuth2UserService naverOAuth2UserService) {
        this.naverOAuth2UserService = naverOAuth2UserService;
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
                        .userInfoEndpoint(cfg -> cfg.userService(naverOAuth2UserService)));

        return http.build();
    }
}
