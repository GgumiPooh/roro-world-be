package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import com.ggumipooh.hanroroworld.be.security.CustomOAuth2UserService;
import com.ggumipooh.hanroroworld.be.security.JwtAuthenticationFilter;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import com.ggumipooh.hanroroworld.be.util.CookieUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        private final CustomOAuth2UserService customOAuth2UserService;
        private final TokenService tokenService;
        private final UserRepository userRepository;
        private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
        private String frontendUrl;

        @org.springframework.beans.factory.annotation.Value("${app.frontend.url.localhost:http://localhost:5173}")
        private String frontendUrlLocalhost;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        TokenService tokenService,
                        UserRepository userRepository,
                        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.tokenService = tokenService;
                this.userRepository = userRepository;
                this.accessTokenResponseClient = accessTokenResponseClient;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // CSRF: cross-origin 환경에서 Spring CsrfTokenRepository 사용 불가
                                // (프론트에서 백엔드 도메인의 CSRF 쿠키를 읽을 수 없음)
                                // 대신 다음 방어 레이어로 CSRF 보호:
                                // 1) CORS: 허용된 origin만 요청 가능
                                // 2) Content-Type: application/json → CORS preflight 발생 → form POST 차단
                                // 3) SameSite=Lax (로컬) / SameSite=None+Secure (프로덕션) 쿠키 속성
                                // 4) JWT 인증 필터가 모든 요청에서 토큰 검증
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // SecurityContext를 세션이 아닌 요청 속성에만 저장
                                // → JSESSIONID에 인증 정보가 남지 않음 (JWT만으로 인증)
                                // OAuth2 authorization request 저장은 별도 메커니즘이므로 영향 없음
                                .securityContext(sc -> sc
                                                .securityContextRepository(
                                                                new RequestAttributeSecurityContextRepository()))
                                // OAuth2 로그인 흐름에서 세션이 필요하므로 IF_REQUIRED 유지
                                // (authorization request 저장용, SecurityContext 저장과 무관)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .addFilterBefore(jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/oauth2/**", "/login/**", "/api/auth/**",
                                                                "/api/public/**", "/api/uploads/**",
                                                                "/error")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"))
                                                .tokenEndpoint(token -> token
                                                                .accessTokenResponseClient(accessTokenResponseClient))
                                                .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(cfg -> cfg.userService(customOAuth2UserService))
                                                .successHandler((req, res, auth) -> {

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

                                                                boolean isSecure = frontendUrl.startsWith("https");
                                                                // SameSite=None은 Secure=true 필수! 로컬은 Lax 사용
                                                                String sameSite = isSecure ? "None" : "Lax";
                                                                // Cookies: HttpOnly + Secure (Domain 생략 → API 서버 전용)
                                                                CookieUtil.addHttpOnlyCookie(res, "access_token",
                                                                                pair.accessToken(),
                                                                                (int) (pair.accessExpiresAt()
                                                                                                .getEpochSecond()
                                                                                                - java.time.Instant
                                                                                                                .now()
                                                                                                                .getEpochSecond()),
                                                                                "/", isSecure, sameSite, null);
                                                                CookieUtil.addHttpOnlyCookie(res, "refresh_token",
                                                                                pair.refreshToken(),
                                                                                (int) (pair.refreshExpiresAt()
                                                                                                .getEpochSecond()
                                                                                                - java.time.Instant
                                                                                                                .now()
                                                                                                                .getEpochSecond()),
                                                                                "/api/auth", isSecure, sameSite,
                                                                                null);
                                                        }
                                                        // 세션 무효화 (JWT만 사용)
                                                        req.getSession().invalidate();

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
                                                        log.error("OAuth2 login failed", ex);
                                                        res.sendRedirect(frontendUrl
                                                                        + "/login?oauth2_error=login_failed");
                                                }));
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(frontendUrl, frontendUrlLocalhost));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin", "X-Requested-With"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
