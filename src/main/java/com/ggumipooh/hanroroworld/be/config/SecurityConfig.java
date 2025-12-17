package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.security.CustomOAuth2UserService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import com.ggumipooh.hanroroworld.be.util.CookieUtil;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, TokenService tokenService,
            UserRepository userRepository) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                }) // ✅ Security에서 CORS 활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/oauth2/**", "/login/**", "/api/auth/**", "/api/public/**",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(cfg -> cfg.userService(customOAuth2UserService))
                        .successHandler((req, res, auth) -> {
                            // Issue our own tokens on successful OAuth login
                            String provider = (auth instanceof OAuth2AuthenticationToken t)
                                    ? t.getAuthorizedClientRegistrationId()
                                    : "unknown";
                            String providerId = auth.getName();
                            User user = userRepository.findByProviderAndProviderId(provider, providerId)
                                    .orElse(null);
                            if (user != null) {
                                var pair = tokenService.issueTokens(user);
                                // Cookies: HttpOnly + Secure
                                CookieUtil.addHttpOnlyCookie(res, "access_token", pair.accessToken(),
                                        (int) (pair.accessExpiresAt().getEpochSecond()
                                                - java.time.Instant.now().getEpochSecond()),
                                        "/", true, "Lax");
                                CookieUtil.addHttpOnlyCookie(res, "refresh_token", pair.refreshToken(),
                                        (int) (pair.refreshExpiresAt().getEpochSecond()
                                                - java.time.Instant.now().getEpochSecond()),
                                        "/api/auth", true, "Strict");
                            }
                            res.sendRedirect("http://localhost:5173");
                        })
                        .failureHandler((req, res, ex) -> {
                            ex.printStackTrace();
                            res.sendRedirect("http://localhost:5173/login?oauth2_error=" +
                                    java.net.URLEncoder.encode(ex.getMessage(),
                                            java.nio.charset.StandardCharsets.UTF_8));
                        }));
        return http.build();
    }
}