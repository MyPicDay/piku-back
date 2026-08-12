package com.pikume.back.user.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User avatar reference")
class UserAvatarReferenceTest {

	@Test
	@DisplayName("Character가 해석한 이미지 참조와 접근 속성을 변경하지 않는다")
	void preservesResolvedCharacterImageReference() {
		UserAvatarReference reference = new UserAvatarReference("generated.webp", false, false);

		assertThat(reference.value()).isEqualTo("generated.webp");
		assertThat(reference.absoluteUrl()).isFalse();
		assertThat(reference.publiclyAccessible()).isFalse();
	}
}
