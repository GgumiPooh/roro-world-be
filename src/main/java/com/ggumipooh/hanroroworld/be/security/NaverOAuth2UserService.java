package com.ggumipooh.hanroroworld.be.security;

import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NaverOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;

    public NaverOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response != null) {
            attributes.putAll(response);
            attributes.remove("response");
        }

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "naver"
        String providerUserId = String.valueOf(attributes.get("id"));

        upsertUser(provider, providerUserId, attributes);

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        return new DefaultOAuth2User(authorities, attributes, "id");
    }

    private void upsertUser(String provider, String providerUserId, Map<String, Object> attributes) {
        userRepository.findByProviderAndProviderId(provider, providerUserId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .provider(provider)
                            .providerId(providerUserId)
                            .nickname((String) attributes.getOrDefault("name", "naver_" + providerUserId))
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
