package com.pikume.back.character.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.pikume.back.character.domain.vo.CharacterCreationType;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Character Domain")
class CharacterTest {

	@Nested
	@DisplayName("AI 캐릭터 생성")
	class AiCharacterCreation {

		@Test
		@DisplayName("userId와 이미지 참조로 AI 캐릭터를 생성한다")
		void createsAiCharacterWithUserId() {
			Character character = Character.aiGenerated("user-42", "ai_image.png");

			assertThat(character.getUserId()).isEqualTo("user-42");
			assertThat(character.getImageReference()).isEqualTo("ai_image.png");
			assertThat(character.getType()).isEqualTo(CharacterCreationType.AI_GENERATED);
		}

		@Test
		@DisplayName("AI 캐릭터는 User 객체 없이 userId 문자열만으로 생성 가능하다")
		void aiCharacterDoesNotRequireUserObject() {
			String userId = "user-123";

			Character character = Character.aiGenerated(userId, "generated.png");

			assertThat(character.getUserId()).isEqualTo(userId);
			assertThat(character.getType()).isEqualTo(CharacterCreationType.AI_GENERATED);
		}

		@Test
		@DisplayName("AI 생성 캐릭터는 사용자 식별자가 필요하다")
		void requiresUserId() {
			assertThatThrownBy(() -> Character.aiGenerated(" ", "generated.png"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("고정 캐릭터 생성")
	class FixedCharacterCreation {

		@Test
		@DisplayName("이미지 참조로 고정 캐릭터를 생성한다")
		void createsFixedCharacter() {
			Character character = Character.fixed("base_image_1.webp");

			assertThat(character.getImageReference()).isEqualTo("base_image_1.webp");
			assertThat(character.getType()).isEqualTo(CharacterCreationType.FIXED);
			assertThat(character.getUserId()).isNull();
		}

		@Test
		@DisplayName("고정 캐릭터는 빈 이미지 참조로 생성할 수 없다")
		void requiresImageReference() {
			assertThatThrownBy(() -> Character.fixed(" "))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
}
