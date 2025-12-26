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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		// Normalize only for Naver so that 'id' is available at the top-level
		if ("naver".equals(registrationId)) {
			Map<String, Object> attributes = oauth2User.getAttributes();
			@SuppressWarnings("unchecked")
			Map<String, Object> response = (Map<String, Object>) attributes.get("response");

			String providerUserId = response != null ? asString(response.get("id")) : asString(attributes.get("id"));
			String displayName = response != null ? asString(response.get("nickname")) : null;

			// If provider user id is missing, return original user to avoid null 'id'
			// attribute errors
			if (providerUserId == null) {
				return oauth2User;
			}

			// Persist only on first sign-in: nickname, provider, providerId
			final String nicknameToSave = (displayName == null || displayName.isBlank())
					? ("naver_" + providerUserId)
					: displayName;
			boolean isNewUser = userRepository.findByProviderAndProviderId(registrationId, providerUserId).isEmpty();
			if (isNewUser) {
				userRepository.save(Objects.requireNonNull(
						User.builder()
								.name(nicknameToSave)
								.provider(registrationId)
								.providerId(providerUserId)
								.build()));
			}

			Map<String, Object> normalized = new HashMap<>();
			// maintain both keys so downstream code can use either
			normalized.put("id", providerUserId);
			normalized.put("response", providerUserId);
			normalized.put("name", nicknameToSave);
			normalized.put("isNewUser", isNewUser);

			Collection<GrantedAuthority> authorities = Collections
					.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
			return new DefaultOAuth2User(authorities, normalized, "response");

		} else if ("kakao".equals(registrationId)) {
			Map<String, Object> attributes = oauth2User.getAttributes();
			System.out.println("[KAKAO raw attributes] " + attributes);
			@SuppressWarnings("unchecked")
			Map<String, Object> kakaoResponse = (Map<String, Object>) attributes.get("kakao_account");
			System.out.println("[KAKAO response] " + kakaoResponse);
			String providerUserId = asString(attributes.get("id"));
			String nickname = null;
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
			System.out.println("[KAKAO properties] " + properties);
			if (properties != null) {
				nickname = asString(properties.get("nickname"));
			}
			if (nickname == null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
				System.out.println("[KAKAO kakao_account] " + kakaoAccount);
				if (kakaoAccount != null) {
					@SuppressWarnings("unchecked")
					Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
					System.out.println("[KAKAO profile] " + profile);
					if (profile != null) {
						nickname = asString(profile.get("nickname"));
					}
				}
			}

			// If provider user id is missing, return original user to avoid null 'id'
			// attribute errors
			if (providerUserId == null) {
				return oauth2User;
			}
			final String nicknameToSave = (nickname == null || nickname.isBlank())
					? ("kakao_" + providerUserId)
					: nickname;
			boolean isNewUser = userRepository.findByProviderAndProviderId(registrationId, providerUserId).isEmpty();
			if (isNewUser) {
				userRepository.save(Objects.requireNonNull(
						User.builder()
								.nickname(nicknameToSave)
								.provider(registrationId)
								.providerId(providerUserId)
								.build()));
			}

			Map<String, Object> normalized = new HashMap<>();
			normalized.put("id", providerUserId);
			normalized.put("name", nicknameToSave);
			normalized.put("isNewUser", isNewUser);

			Collection<GrantedAuthority> authorities = Collections
					.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
			return new DefaultOAuth2User(authorities, normalized, "id");
		}
		String providerId = oauth2User.getName(); // usually the subject or provider user id
		// Try common nickname keys, then fallback
		String rawNickname = asString(oauth2User.getAttributes().get("nickname"));
		if (rawNickname == null) {
			rawNickname = asString(oauth2User.getAttributes().get("name"));
		}
		if (providerId != null) {
			if (userRepository.findByProviderAndProviderId(registrationId, providerId).isEmpty()) {
				userRepository.save(Objects.requireNonNull(
						User.builder()
								.nickname((rawNickname == null || rawNickname.isBlank())
										? (registrationId + "_" + providerId)
										: rawNickname)
								.provider(registrationId)
								.providerId(providerId)
								.build()));
			}
		}
		return oauth2User;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
