package com.ggumipooh.hanroroworld.be.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom converter for Naver OAuth2 token response.
 * Naver returns expires_in as String, but Spring Security expects Number.
 */
public class NaverOAuth2AccessTokenResponseConverter 
        implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> source) {
        String accessToken = (String) source.get(OAuth2ParameterNames.ACCESS_TOKEN);
        String refreshToken = (String) source.get(OAuth2ParameterNames.REFRESH_TOKEN);
        String tokenType = (String) source.get(OAuth2ParameterNames.TOKEN_TYPE);
        
        // Handle expires_in as String or Number
        long expiresIn = 0;
        Object expiresInValue = source.get(OAuth2ParameterNames.EXPIRES_IN);
        if (expiresInValue instanceof String) {
            expiresIn = Long.parseLong((String) expiresInValue);
        } else if (expiresInValue instanceof Number) {
            expiresIn = ((Number) expiresInValue).longValue();
        }

        OAuth2AccessToken.TokenType accessTokenType = OAuth2AccessToken.TokenType.BEARER;
        if (tokenType != null && tokenType.equalsIgnoreCase("mac")) {
            // Naver uses Bearer, but just in case
            accessTokenType = OAuth2AccessToken.TokenType.BEARER;
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!OAuth2ParameterNames.ACCESS_TOKEN.equals(entry.getKey()) &&
                !OAuth2ParameterNames.REFRESH_TOKEN.equals(entry.getKey()) &&
                !OAuth2ParameterNames.TOKEN_TYPE.equals(entry.getKey()) &&
                !OAuth2ParameterNames.EXPIRES_IN.equals(entry.getKey()) &&
                !OAuth2ParameterNames.SCOPE.equals(entry.getKey())) {
                additionalParameters.put(entry.getKey(), entry.getValue());
            }
        }

        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse
                .withToken(accessToken)
                .tokenType(accessTokenType)
                .expiresIn(expiresIn)
                .additionalParameters(additionalParameters);

        if (refreshToken != null) {
            builder.refreshToken(refreshToken);
        }

        return builder.build();
    }
}

