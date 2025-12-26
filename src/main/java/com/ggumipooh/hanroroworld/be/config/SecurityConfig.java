package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import com.ggumipooh.hanroroworld.be.security.CustomOAuth2UserService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import com.ggumipooh.hanroroworld.be.util.CookieUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final TokenService tokenService;
        private final UserRepository userRepository;
        private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

        @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
        private String frontendUrl;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        TokenService tokenService,
                        UserRepository userRepository,
                        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.tokenService = tokenService;
                this.userRepository = userRepository;
                this.accessTokenResponseClient = accessTokenResponseClient;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> {
                                }) // ✅ Security에서 CORS 활성화
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/health", "/oauth2/**", "/login/**", "/api/auth/**",
                                                                "/api/public/**",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/song/**/comments")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"))
                                                .tokenEndpoint(token -> token
                                                                .accessTokenResponseClient(accessTokenResponseClient))
                                                .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(cfg -> cfg.userService(customOAuth2UserService))
                                                .successHandler((req, res, auth) -> {
                                                        // Issue our own tokens on successful OAuth login
                                                        String provider = (auth instanceof OAuth2AuthenticationToken t)
                                                                        ? t.getAuthorizedClientRegistrationId()
                                                                        : "unknown";
                                                        String providerId = auth.getName();
                                                        User user = userRepository
                                                                        .findByProviderAndProviderId(provider,
                                                                                        providerId)
                                                                        .orElse(null);
                                                        if (user != null) {
                                                                var pair = tokenService.issueTokens(user);
                                                                // Cookies: HttpOnly + Secure
                                                                CookieUtil.addHttpOnlyCookie(res, "access_token",
                                                                                pair.accessToken(),
                                                                                (int) (pair.accessExpiresAt()
                                                                                                .getEpochSecond()
                                                                                                - java.time.Instant
                                                                                                                .now()
                                                                                                                .getEpochSecond()),
                                                                                "/", true, "None");
                                                                CookieUtil.addHttpOnlyCookie(res, "refresh_token",
                                                                                pair.refreshToken(),
                                                                                (int) (pair.refreshExpiresAt()
                                                                                                .getEpochSecond()
                                                                                                - java.time.Instant
                                                                                                                .now()
                                                                                                                .getEpochSecond()),
                                                                                "/api/auth", true, "None");
                                                        }
                                                        // 신규 유저면 동의 페이지로, 기존 유저면 홈으로
                                                        Object isNewUserAttr = auth
                                                                        .getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User
                                                                                        ? oauth2User.getAttributes()
                                                                                                        .get("isNewUser")
                                                                                        : null;
                                                        boolean isNewUser = Boolean.TRUE.equals(isNewUserAttr);
                                                        String redirectUrl = isNewUser
                                                                        ? frontendUrl + "/signup-complete"
                                                                        : frontendUrl + "/";
                                                        res.sendRedirect(redirectUrl);
                                                })
                                                .failureHandler((req, res, ex) -> {
                                                        ex.printStackTrace();
                                                        res.sendRedirect(frontendUrl + "/login?oauth2_error=" +
                                                                        java.net.URLEncoder.encode(ex.getMessage(),
                                                                                        java.nio.charset.StandardCharsets.UTF_8));
                                                }));
                return http.build();
        }
}
