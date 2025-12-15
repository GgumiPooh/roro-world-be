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
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
	private final UserRepository userRepository;

	public CustomOAuth2UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = delegate.loadUser(userRequest);
		Map<String, Object> normalized = normalizeAttributes(
				userRequest.getClientRegistration().getRegistrationId(),
				oauth2User.getAttributes());

		String provider = userRequest.getClientRegistration().getRegistrationId();
		String providerUserId = String.valueOf(normalized.get("id"));

		upsertUser(provider, providerUserId, normalized);

		Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
		return new DefaultOAuth2User(authorities, normalized, "id");
	}

	private Map<String, Object> normalizeAttributes(String provider, Map<String, Object> attributes) {
		Map<String, Object> result = new HashMap<>();
		if ("naver".equals(provider)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = (Map<String, Object>) attributes.get("response");
			if (response != null) {
				result.putAll(response);
			}
			copyIfPresent(attributes, result, "id");
			result.putIfAbsent("name", result.get("name"));
			result.putIfAbsent("email", result.get("email"));
			result.putIfAbsent("profile_image", result.get("profile_image"));
		} else if ("kakao".equals(provider)) {
			// Kakao structure: id (top), properties{nickname, profile_image},
			// kakao_account{email, profile{nickname, profile_image_url}}
			Object id = attributes.get("id");
			result.put("id", String.valueOf(id));

			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
			@SuppressWarnings("unchecked")
			Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
			@SuppressWarnings("unchecked")
			Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile")
					: null;

			String nickname = null;
			String profileImage = null;
			String email = null;

			if (properties != null) {
				nickname = getString(properties, "nickname");
				profileImage = getString(properties, "profile_image");
			}
			if (profile != null) {
				if (nickname == null)
					nickname = getString(profile, "nickname");
				// profile image url key may be profile_image_url
				if (profileImage == null)
					profileImage = getString(profile, "profile_image_url");
			}
			if (kakaoAccount != null) {
				if (email == null)
					email = getString(kakaoAccount, "email");
			}

			result.put("name", nickname != null ? nickname : "kakao_" + result.get("id"));
			if (email != null)
				result.put("email", email);
			if (profileImage != null)
				result.put("profile_image", profileImage);
		} else {
			// default passthrough
			result.putAll(attributes);
		}
		return result;
	}

	private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
		if (source.containsKey(key)) {
			target.put(key, source.get(key));
		}
	}

	private static String getString(Map<String, Object> map, String key) {
		Object v = map.get(key);
		return v == null ? null : String.valueOf(v);
	}

	private void upsertUser(String provider, String providerUserId, Map<String, Object> attributes) {
		userRepository.findByProviderAndProviderId(provider, providerUserId)
				.orElseGet(() -> {
					User newUser = User.builder()
							.provider(provider)
							.providerId(providerUserId)
							.nickname((String) attributes.getOrDefault("name", provider + "_" + providerUserId))
							.build();
					return userRepository.save(newUser);
				});
	}
}
