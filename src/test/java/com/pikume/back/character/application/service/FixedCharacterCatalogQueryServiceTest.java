package com.pikume.back.character.application.service;

import com.pikume.back.character.application.exception.CharacterException;
import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import com.pikume.back.character.application.port.out.LoadFixedCharactersPort;
import com.pikume.back.character.domain.Character;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FixedCharacterCatalogQueryService")
class FixedCharacterCatalogQueryServiceTest {

	@InjectMocks
	private FixedCharacterCatalogQueryService service;

	@Mock
	private LoadFixedCharactersPort loadFixedCharactersPort;

	@Mock
	private CanonicalizeFixedCharacterObjectKeyPort canonicalizeFixedCharacterObjectKeyPort;

	@Test
	@DisplayName("같은 캐릭터의 PNG와 WebP가 있으면 WebP 참조를 우선한다")
	void prefersWebpReferenceForSameCharacter() {
		Character png = Character.fixed("base_image_1.png");
		Character webp = Character.fixed("base_image_1.webp");
		Character fallback = Character.fixed("base_image_2.png");
		given(loadFixedCharactersPort.loadFixedCharacters()).willReturn(List.of(png, webp, fallback));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.png"))
				.willReturn("public/characters/fixed/base_image_1.png");
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.webp"))
				.willReturn("public/characters/fixed/base_image_1.webp");
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_2.png"))
				.willReturn("public/characters/fixed/base_image_2.png");

		var result = service.queryFixedCharacters();

		assertThat(result).extracting(item -> item.imageReference())
				.containsExactly(
						"public/characters/fixed/base_image_1.webp",
						"public/characters/fixed/base_image_2.png");
	}

	@Test
	@DisplayName("잘못된 저장 참조는 목록에서 건너뛴다")
	void skipsInvalidStoredReference() {
		Character invalid = Character.fixed("invalid.png");
		given(loadFixedCharactersPort.loadFixedCharacters()).willReturn(List.of(invalid));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("invalid.png"))
				.willThrow(new IllegalArgumentException("invalid"));

		assertThat(service.queryFixedCharacters()).isEmpty();
	}

	@Test
	@DisplayName("카탈로그 저장소 조회 실패는 기술 중립 Character 오류로 변환한다")
	void translatesCatalogPersistenceFailure() {
		given(loadFixedCharactersPort.loadFixedCharacters())
				.willThrow(new IllegalStateException("database detail"));

		assertThatThrownBy(() -> service.queryFixedCharacters())
				.isInstanceOf(CharacterException.class)
				.hasMessage("캐릭터 목록을 불러올 수 없습니다.");
	}
}
