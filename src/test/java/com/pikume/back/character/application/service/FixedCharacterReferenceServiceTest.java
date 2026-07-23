package com.pikume.back.character.application.service;

import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import com.pikume.back.character.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.character.domain.Character;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FixedCharacterReferenceService")
class FixedCharacterReferenceServiceTest {

	@InjectMocks
	private FixedCharacterReferenceService service;

	@Mock
	private LoadCharacterReferencePort loadCharacterReferencePort;

	@Mock
	private CanonicalizeFixedCharacterObjectKeyPort canonicalizeFixedCharacterObjectKeyPort;

	@Test
	@DisplayName("고정 캐릭터의 저장 참조를 canonical object key로 해석한다")
	void resolvesFixedCharacterReference() {
		given(loadCharacterReferencePort.loadCharacterReference(1L))
				.willReturn(Optional.of(Character.fixed("base_image_1.webp")));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base_image_1.webp"))
				.willReturn("public/characters/fixed/base_image_1.webp");

		assertThat(service.resolveFixedCharacterObjectKey(1L))
				.contains("public/characters/fixed/base_image_1.webp");
		assertThat(service.findFixedCharacterObjectKey(1L))
				.contains("public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("AI 생성 캐릭터는 고정 캐릭터 참조로 반환하지 않는다")
	void doesNotResolveAiGeneratedCharacter() {
		given(loadCharacterReferencePort.loadCharacterReference(2L))
				.willReturn(Optional.of(Character.aiGenerated("user-1", "ai.png")));

		assertThat(service.resolveFixedCharacterObjectKey(2L)).isEmpty();
	}

	@Test
	@DisplayName("유효하지 않은 식별자는 저장소를 조회하지 않는다")
	void doesNotLoadInvalidIdentifier() {
		assertThat(service.resolveFixedCharacterObjectKey(null)).isEmpty();
		assertThat(service.resolveFixedCharacterObjectKey(0L)).isEmpty();

		then(loadCharacterReferencePort).should(never()).loadCharacterReference(any());
	}

	@Test
	@DisplayName("잘못된 저장 참조는 빈 결과로 변환한다")
	void returnsEmptyForInvalidStoredReference() {
		given(loadCharacterReferencePort.loadCharacterReference(1L))
				.willReturn(Optional.of(Character.fixed("invalid.png")));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("invalid.png"))
				.willThrow(new IllegalArgumentException("invalid"));

		assertThat(service.resolveFixedCharacterObjectKey(1L)).isEmpty();
	}
}
