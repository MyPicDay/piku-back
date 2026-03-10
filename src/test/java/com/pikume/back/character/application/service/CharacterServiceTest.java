package com.pikume.back.character.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.character.application.dto.CharacterImageContent;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.character.application.port.out.CharacterImageStoragePort;
import com.pikume.back.character.application.port.out.LoadCharacterPort;
import com.pikume.back.character.application.port.out.SaveCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.exception.CharacterNotFoundException;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterService")
class CharacterServiceTest {

	@InjectMocks
	private CharacterService characterService;

	@Mock
	private LoadCharacterPort loadCharacterPort;

	@Mock
	private SaveCharacterPort saveCharacterPort;

	@Mock
	private CharacterImageStoragePort characterImageStoragePort;

	@Nested
	@DisplayName("getFixedCharacters")
	class GetFixedCharacters {

		@Test
		@DisplayName("고정 캐릭터 목록을 정상 반환한다")
		void returnsFixedCharacterList() {
			Character c1 = new Character("base_image_1.png", CharacterCreationType.FIXED);
			Character c2 = new Character("base_image_2.png", CharacterCreationType.FIXED);
			given(loadCharacterPort.findByType(CharacterCreationType.FIXED)).willReturn(List.of(c1, c2));

			List<CharacterResult> result = characterService.getFixedCharacters();

			assertThat(result).hasSize(2);
			assertThat(result).allSatisfy(c -> assertThat(c.type()).isEqualTo(CharacterCreationType.FIXED));
		}

		@Test
		@DisplayName("고정 캐릭터가 없으면 빈 리스트를 반환한다")
		void returnsEmptyListWhenNone() {
			given(loadCharacterPort.findByType(CharacterCreationType.FIXED)).willReturn(List.of());

			List<CharacterResult> result = characterService.getFixedCharacters();

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("getCharacterById")
	class GetCharacterById {

		@Test
		@DisplayName("존재하는 ID로 조회 시 캐릭터를 반환한다")
		void returnsCharacterWhenExists() {
			Character character = new Character("test.png", CharacterCreationType.FIXED);
			given(loadCharacterPort.findById(1L)).willReturn(Optional.of(character));

			CharacterResult result = characterService.getCharacterById(1L);

			assertThat(result.imageUrl()).isEqualTo("test.png");
		}

		@Test
		@DisplayName("존재하지 않는 ID로 조회 시 CharacterNotFoundException 발생")
		void throwsExceptionWhenNotFound() {
			given(loadCharacterPort.findById(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> characterService.getCharacterById(999L))
					.isInstanceOf(CharacterNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("getFixedCharacterImageUrl")
	class GetFixedCharacterImageUrl {

		@Test
		@DisplayName("고정 캐릭터의 이미지 URL을 정상 반환한다")
		void returnsUrlForFixedCharacter() {
			Character character = new Character("base_image_1.png", CharacterCreationType.FIXED);
			given(loadCharacterPort.findById(1L)).willReturn(Optional.of(character));

			String url = characterService.getFixedCharacterImageUrl(1L);

			assertThat(url).isEqualTo("characters/fixed/base_image_1.png");
		}

		@Test
		@DisplayName("AI 생성 캐릭터 ID로 조회 시 null을 반환한다")
		void returnsNullForAiCharacter() {
			Character aiCharacter = new Character("user-1", "ai_image.png");
			given(loadCharacterPort.findById(2L)).willReturn(Optional.of(aiCharacter));

			String url = characterService.getFixedCharacterImageUrl(2L);

			assertThat(url).isNull();
		}

		@Test
		@DisplayName("존재하지 않는 캐릭터 ID로 조회 시 null을 반환한다")
		void returnsNullWhenNotFound() {
			given(loadCharacterPort.findById(999L)).willReturn(Optional.empty());

			String url = characterService.getFixedCharacterImageUrl(999L);

			assertThat(url).isNull();
		}
	}

	@Nested
	@DisplayName("isCharacterFixedImageExists")
	class IsCharacterFixedImageExists {

		@Test
		@DisplayName("고정 캐릭터가 존재하면 true를 반환한다")
		void returnsTrueForFixedCharacter() {
			Character character = new Character("base.png", CharacterCreationType.FIXED);
			given(loadCharacterPort.findById(1L)).willReturn(Optional.of(character));

			assertThat(characterService.isCharacterFixedImageExists(1L)).isTrue();
		}

		@Test
		@DisplayName("AI 생성 캐릭터면 false를 반환한다")
		void returnsFalseForAiCharacter() {
			Character aiCharacter = new Character("user-1", "ai.png");
			given(loadCharacterPort.findById(2L)).willReturn(Optional.of(aiCharacter));

			assertThat(characterService.isCharacterFixedImageExists(2L)).isFalse();
		}

		@Test
		@DisplayName("존재하지 않으면 false를 반환한다")
		void returnsFalseWhenNotFound() {
			given(loadCharacterPort.findById(999L)).willReturn(Optional.empty());

			assertThat(characterService.isCharacterFixedImageExists(999L)).isFalse();
		}
	}

	@Nested
	@DisplayName("createAiCharacter")
	class CreateAiCharacter {

		@Test
		@DisplayName("String userId로 AI 캐릭터를 정상 생성한다")
		void createsAiCharacterWithUserIdOnly() {
			String userId = "user-42";
			UploadedFileData imageFile = new UploadedFileData("character.png", "image/png", "data".getBytes());
			String desiredName = "my_character";

			given(characterImageStoragePort.saveCharacterImage(imageFile, CharacterCreationType.AI_GENERATED, userId, desiredName))
					.willReturn("saved_image.png");
			given(saveCharacterPort.save(any(Character.class)))
					.willAnswer(inv -> inv.getArgument(0));

			CharacterResult result = characterService.createAiCharacter(userId, imageFile, desiredName);

			assertThat(result.userId()).isEqualTo("user-42");
			assertThat(result.imageUrl()).isEqualTo("saved_image.png");
			assertThat(result.type()).isEqualTo(CharacterCreationType.AI_GENERATED);
		}

		@Test
		@DisplayName("userId가 null이면 IllegalArgumentException 발생")
		void throwsWhenUserIdIsNull() {
			assertThatThrownBy(() -> characterService.createAiCharacter(null,
					new UploadedFileData("a.png", "image/png", "x".getBytes()), "name"))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("userId가 빈 문자열이면 IllegalArgumentException 발생")
		void throwsWhenUserIdIsBlank() {
			assertThatThrownBy(() -> characterService.createAiCharacter("  ",
					new UploadedFileData("a.png", "image/png", "x".getBytes()), "name"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("saveFixedCharacter")
	class SaveFixedCharacter {

		@Test
		@DisplayName("고정 캐릭터를 정상 저장한다")
		void savesCharacter() {
			Character character = new Character("new.png", CharacterCreationType.FIXED);
			given(saveCharacterPort.save(any(Character.class))).willReturn(character);

			CharacterResult saved = characterService.saveFixedCharacter("new.png");

			assertThat(saved.imageUrl()).isEqualTo("new.png");
			assertThat(saved.type()).isEqualTo(CharacterCreationType.FIXED);
			then(saveCharacterPort).should().save(any(Character.class));
		}
	}

	@Nested
	@DisplayName("getFixedCharacterImage")
	class GetFixedCharacterImage {

		@Test
		@DisplayName("고정 캐릭터 이미지를 application DTO로 로드한다")
		void loadsResourceForFixedCharacter() {
			CharacterImageContent content = new CharacterImageContent("base.png", "image/png", "img".getBytes());
			given(characterImageStoragePort.loadCharacterImage(CharacterCreationType.FIXED, null, "base.png"))
					.willReturn(content);

			CharacterImageContent result = characterService.getFixedCharacterImage("base.png");

			assertThat(result).isEqualTo(content);
		}
	}
}
