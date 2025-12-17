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
                        .requestMatchers("/health", "/oauth2/**", "/login/**", "/api/auth/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(cfg -> cfg.userService(customOAuth2UserService))
                        .successHandler((req, res, auth) -> res.sendRedirect("http://localhost:5173"))
                        .failureHandler((req, res, ex) -> {
                            ex.printStackTrace();
                            res.sendRedirect("/login?oauth2_error=" +
                                    java.net.URLEncoder.encode(ex.getMessage(),
                                            java.nio.charset.StandardCharsets.UTF_8));
                        }));
        return http.build();
    }
}