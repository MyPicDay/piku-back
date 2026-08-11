package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.character.application.dto.UsableCharacterReference;
import com.pikume.back.character.application.port.in.ResolveUsableCharacterReferenceUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterReferenceAdapterForCreative")
class CharacterReferenceAdapterForCreativeTest {

	@Mock
	private ResolveUsableCharacterReferenceUseCase resolveUsableCharacterReferenceUseCase;

	@Test
	@DisplayName("Character 공개 결과를 Creative 선택 캐릭터 저장 참조로 번역한다")
	void translatesUsableCharacterReference() {
		given(resolveUsableCharacterReferenceUseCase.resolveUsableReference("user-1", 7L))
				.willReturn(Optional.of(new UsableCharacterReference(
						"public/characters/fixed/base.webp")));
		CharacterReferenceAdapterForCreative adapter = adapter();

		assertThat(adapter.loadSelectedCharacterReference("user-1", 7L))
				.contains("public/characters/fixed/base.webp");
	}

	@Test
	@DisplayName("Character 사용 불가 결과를 Creative의 빈 참조로 번역한다")
	void translatesUnavailableCharacterReference() {
		given(resolveUsableCharacterReferenceUseCase.resolveUsableReference("user-1", 7L))
				.willReturn(Optional.empty());

		assertThat(adapter().loadSelectedCharacterReference("user-1", 7L)).isEmpty();
	}

	@Test
	@DisplayName("Character 기술 장애를 사용 불가 결과로 숨기지 않는다")
	void propagatesUnexpectedCharacterFailure() {
		IllegalStateException failure = new IllegalStateException("database unavailable");
		given(resolveUsableCharacterReferenceUseCase.resolveUsableReference("user-1", 7L))
				.willThrow(failure);

		assertThatThrownBy(() -> adapter().loadSelectedCharacterReference("user-1", 7L))
				.isSameAs(failure);
	}

	private CharacterReferenceAdapterForCreative adapter() {
		return new CharacterReferenceAdapterForCreative(resolveUsableCharacterReferenceUseCase);
	}
}
