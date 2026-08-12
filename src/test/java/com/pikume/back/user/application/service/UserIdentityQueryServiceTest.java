package com.pikume.back.user.application.service;

import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.out.LoadUserForAuthenticationPort;
import com.pikume.back.user.application.port.out.ResolveAvatarCharacterReferencesPort;
import com.pikume.back.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserIdentityQueryService")
class UserIdentityQueryServiceTest {

	private UserIdentityQueryService userIdentityQueryService;

	@Mock
	private LoadUserForAuthenticationPort loadUserForAuthenticationPort;
	@Mock
	private ResolveAvatarCharacterReferencesPort resolveAvatarCharacterReferencesPort;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		userIdentityQueryService = new UserIdentityQueryService(
				loadUserForAuthenticationPort,
				new UserAvatarReferenceResolver(resolveAvatarCharacterReferencesPort));
	}

	@Test
	@DisplayName("사용자 ID로 인증에 필요한 사용자 식별 정보를 조회한다")
	void queryUserIdentityByIdReturnsUserIdentity() {
		User user = new User(
				"user-id",
				"user@example.com",
				"encoded-password",
				"pikume",
				1L);
		given(loadUserForAuthenticationPort.loadForSession("user-id")).willReturn(Optional.of(user));
		given(resolveAvatarCharacterReferencesPort.resolveAvatarCharacterReferences(
				java.util.Set.of(new AvatarCharacterSelection("user-id", 1L))))
				.willReturn(List.of(new AvatarCharacterReference(
						"user-id", 1L,
						new UserAvatarReference(
								"public/characters/fixed/base_image_1.png", false, true))));

		var result = userIdentityQueryService.queryUserIdentityById("user-id");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().passwordHash()).isEqualTo("encoded-password");
		assertThat(result.get().avatarReference()).isEqualTo(new UserAvatarReference(
				"public/characters/fixed/base_image_1.png", false, true));
	}

	@Test
	@DisplayName("이메일로 인증에 필요한 사용자 식별 정보를 조회한다")
	void queryUserIdentityByEmailReturnsUserIdentity() {
		User user = new User(
				"user-id",
				"user@example.com",
				"encoded-password",
				"pikume",
				1L);
		given(loadUserForAuthenticationPort.loadForLogin("user@example.com")).willReturn(Optional.of(user));
		given(resolveAvatarCharacterReferencesPort.resolveAvatarCharacterReferences(
				java.util.Set.of(new AvatarCharacterSelection("user-id", 1L))))
				.willReturn(List.of(new AvatarCharacterReference(
						"user-id", 1L,
						new UserAvatarReference(
								"public/characters/fixed/base_image_1.png", false, true))));

		var result = userIdentityQueryService.queryUserIdentityByEmail("user@example.com");

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo("user-id");
		assertThat(result.get().passwordHash()).isEqualTo("encoded-password");
		assertThat(result.get().nickname()).isEqualTo("pikume");
	}
}
