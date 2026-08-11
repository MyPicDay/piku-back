package com.pikume.back.character.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CharacterImageReference")
class CharacterImageReferenceTest {

	@Test
	@DisplayName("안전한 상대 Object Key를 생성 참조로 제공한다")
	void providesSafeObjectKey() {
		CharacterImageReference reference = CharacterImageReference.of(
				" private/characters/user-1/generated.webp ");

		assertThat(reference.toUsableObjectKey())
				.contains("private/characters/user-1/generated.webp");
	}

	@Test
	@DisplayName("절대 URL과 경로 이탈 참조는 생성용 Object Key로 제공하지 않는다")
	void rejectsUnsafeObjectKeys() {
		assertThat(CharacterImageReference.of("https://assets.example.com/character.webp")
				.toUsableObjectKey()).isEmpty();
		assertThat(CharacterImageReference.of("private/../character.webp")
				.toUsableObjectKey()).isEmpty();
		assertThat(CharacterImageReference.of("private\\character.webp")
				.toUsableObjectKey()).isEmpty();
		assertThat(CharacterImageReference.of("/private/character.webp")
				.toUsableObjectKey()).isEmpty();
	}
}
