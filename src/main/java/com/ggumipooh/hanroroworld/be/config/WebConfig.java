package com.ggumipooh.hanroroworld.be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS 설정은 SecurityConfig.corsConfigurationSource()에서 관리
// Spring Security 레벨에서 CORS를 처리해야 Set-Cookie 등 응답 헤더가 정상 동작
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
