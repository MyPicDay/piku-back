package com.pikume.back.user.auth.adapter.out.character;

import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterAdapterForAuth")
class CharacterAdapterForAuthTest {

	@InjectMocks
	private CharacterAdapterForAuth characterAdapterForAuth;

	@Mock
	private GetCharacterUseCase getCharacterUseCase;

	@Test
	@DisplayName("회원가입에서 선택 가능한 고정 캐릭터인지 Character 공개 계약으로 확인한다")
	void checksSelectableFixedCharacter() {
		given(getCharacterUseCase.findFixedCharacterObjectKey(1L))
				.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));

		boolean result = characterAdapterForAuth.isSelectableFixedCharacter(1L);

		assertThat(result).isTrue();
	}
}
