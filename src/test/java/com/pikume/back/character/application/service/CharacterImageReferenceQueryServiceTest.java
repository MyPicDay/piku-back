package com.pikume.back.character.application.service;

import com.pikume.back.character.application.dto.CharacterImageReferenceResult;
import com.pikume.back.character.application.dto.CharacterImageReferenceQuery;
import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import com.pikume.back.character.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.character.domain.Character;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterImageReferenceQueryService")
class CharacterImageReferenceQueryServiceTest {

	@InjectMocks
	private CharacterImageReferenceQueryService service;

	@Mock
	private LoadCharacterReferencePort loadCharacterReferencePort;

	@Mock
	private CanonicalizeFixedCharacterObjectKeyPort canonicalizeFixedCharacterObjectKeyPort;

	@Test
	@DisplayName("고정 캐릭터는 모든 사용자에게, AI 생성 캐릭터는 소유 사용자에게만 반환한다")
	void resolvesOnlyCharacterReferencesAccessibleToEachUser() {
		Character fixed = identified(1L, Character.fixed("base_image_1.webp"));
		Character aiGenerated = identified(2L, Character.aiGenerated("user-1", "generated/user-1.webp"));
		given(loadCharacterReferencePort.loadCharacterReferences(Set.of(1L, 2L)))
				.willReturn(List.of(fixed, aiGenerated));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.webp"))
				.willReturn("public/characters/fixed/base_image_1.webp");

		List<CharacterImageReferenceResult> results = service.queryCharacterImageReferences(
				List.of(
						new CharacterImageReferenceQuery("user-1", 2L),
						new CharacterImageReferenceQuery("user-2", 2L),
						new CharacterImageReferenceQuery("user-1", 1L),
						new CharacterImageReferenceQuery("user-2", 1L),
						new CharacterImageReferenceQuery("user-1", 2L)));

		assertThat(results).containsExactly(
				new CharacterImageReferenceResult(
						"user-1", 2L, "generated/user-1.webp", false, false),
				new CharacterImageReferenceResult(
						"user-1", 1L, "public/characters/fixed/base_image_1.webp", false, true),
				new CharacterImageReferenceResult(
						"user-2", 1L, "public/characters/fixed/base_image_1.webp", false, true));
	}

	@Test
	@DisplayName("존재하지 않는 캐릭터는 결과에서 제외한다")
	void ignoresMissingCharacters() {
		Character fixed = identified(1L, Character.fixed("base_image_1.webp"));
		given(loadCharacterReferencePort.loadCharacterReferences(Set.of(1L, 99L)))
				.willReturn(List.of(fixed));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.webp"))
				.willReturn("public/characters/fixed/base_image_1.webp");

		List<CharacterImageReferenceResult> results = service.queryCharacterImageReferences(
				List.of(
						new CharacterImageReferenceQuery("user-1", 1L),
						new CharacterImageReferenceQuery("user-1", 99L)));

		assertThat(results).containsExactly(
				new CharacterImageReferenceResult(
						"user-1", 1L, "public/characters/fixed/base_image_1.webp", false, true));
	}

	@Test
	@DisplayName("절대 URL은 Character가 표시 가능한 참조로 분류한다")
	void classifiesAbsoluteImageReference() {
		Character aiGenerated = identified(
				2L,
				Character.aiGenerated("user-1", "https://cdn.example.com/generated/user-1.webp"));
		given(loadCharacterReferencePort.loadCharacterReferences(Set.of(2L)))
				.willReturn(List.of(aiGenerated));

		List<CharacterImageReferenceResult> results = service.queryCharacterImageReferences(
				List.of(new CharacterImageReferenceQuery("user-1", 2L)));

		assertThat(results).containsExactly(new CharacterImageReferenceResult(
				"user-1",
				2L,
				"https://cdn.example.com/generated/user-1.webp",
				true,
				false));
	}

	@Test
	@DisplayName("빈 입력은 저장소를 조회하지 않고 빈 결과를 반환한다")
	void returnsEmptyWithoutLoadingForEmptyInput() {
		assertThat(service.queryCharacterImageReferences(List.of())).isEmpty();

		then(loadCharacterReferencePort).should(never()).loadCharacterReferences(Set.of());
	}

	private Character identified(Long id, Character character) {
		ReflectionTestUtils.setField(character, "id", id);
		return character;
	}
}
