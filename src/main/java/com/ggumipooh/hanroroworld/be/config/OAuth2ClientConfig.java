package com.ggumipooh.hanroroworld.be.config;

import com.ggumipooh.hanroroworld.be.security.NaverOAuth2AccessTokenResponseConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * OAuth2 client configuration for handling non-standard token responses (like Naver).
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();

        // Custom message converter for handling Naver's non-standard response
        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseConverter = 
                new OAuth2AccessTokenResponseHttpMessageConverter();
        tokenResponseConverter.setAccessTokenResponseConverter(new NaverOAuth2AccessTokenResponseConverter());

        RestTemplate restTemplate = new RestTemplate(Arrays.asList(
                new FormHttpMessageConverter(),
                tokenResponseConverter
        ));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        client.setRestOperations(restTemplate);
        return client;
    }
}

