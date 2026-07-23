package com.pikume.back.user.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User avatar reference")
class UserAvatarReferenceTest {

	@Test
	@DisplayName("고정 캐릭터 파일명과 이전 경로를 공개 Object Key로 정규화한다")
	void normalizesFixedCharacterReferences() {
		assertThat(UserAvatarReference.fromStoredPath("base_image_1.webp"))
				.isEqualTo(new UserAvatarReference(
						"public/characters/fixed/base_image_1.webp",
						false,
						true));
		assertThat(UserAvatarReference.fromStoredPath("characters/fixed/base_image_1.webp").value())
				.isEqualTo("public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("절대 URL과 일반 Object Key의 현재 의미를 유지한다")
	void keepsAbsoluteUrlsAndNonCharacterObjectKeys() {
		assertThat(UserAvatarReference.fromStoredPath("https://cdn.example.com/avatar.png"))
				.isEqualTo(new UserAvatarReference(
						"https://cdn.example.com/avatar.png",
						true,
						false));
		assertThat(UserAvatarReference.fromStoredPath("avatars/user.png"))
				.isEqualTo(new UserAvatarReference("avatars/user.png", false, false));
	}

	@Test
	@DisplayName("빈 값과 안전하지 않은 고정 캐릭터 경로는 빈 참조로 처리한다")
	void rejectsMissingOrUnsafeFixedCharacterReferences() {
		assertThat(UserAvatarReference.fromStoredPath(null).isEmpty()).isTrue();
		assertThat(UserAvatarReference.fromStoredPath(" ").isEmpty()).isTrue();
		assertThat(UserAvatarReference.fromStoredPath("characters/fixed/group/../bad.png").isEmpty()).isTrue();
		assertThat(UserAvatarReference.fromStoredPath("public/characters/fixed/../bad.png").isEmpty()).isTrue();
	}
}
