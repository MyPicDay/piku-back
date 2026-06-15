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
		@DisplayName("userId와 imageUrl로 AI 캐릭터를 생성한다")
		void createsAiCharacterWithUserId() {
			Character character = new Character("user-42", "ai_image.png");

			assertThat(character.getUserId()).isEqualTo("user-42");
			assertThat(character.getImageUrl()).isEqualTo("ai_image.png");
			assertThat(character.getType()).isEqualTo(CharacterCreationType.AI_GENERATED);
		}

		@Test
		@DisplayName("AI 캐릭터는 User 객체 없이 userId 문자열만으로 생성 가능하다")
		void aiCharacterDoesNotRequireUserObject() {
			String userId = "user-123";

			Character character = new Character(userId, "generated.png");

			assertThat(character.getUserId()).isEqualTo(userId);
			assertThat(character.getType()).isEqualTo(CharacterCreationType.AI_GENERATED);
		}
	}

	@Nested
	@DisplayName("고정 캐릭터 생성")
	class FixedCharacterCreation {

		@Test
		@DisplayName("imageUrl과 FIXED 타입으로 고정 캐릭터를 생성한다")
		void createsFixedCharacter() {
			Character character = new Character("base_image_1.webp", CharacterCreationType.FIXED);

			assertThat(character.getImageUrl()).isEqualTo("base_image_1.webp");
			assertThat(character.getType()).isEqualTo(CharacterCreationType.FIXED);
			assertThat(character.getUserId()).isNull();
		}
	}
}
