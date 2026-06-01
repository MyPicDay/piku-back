package com.pikume.back.user.adapter.out.character;

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
@DisplayName("CharacterAdapterForUser")
class CharacterAdapterForUserTest {

	@InjectMocks
	private CharacterAdapterForUser characterAdapterForUser;

	@Mock
	private GetCharacterUseCase getCharacterUseCase;

	@Test
	@DisplayName("사용자 프로필용 고정 캐릭터 object key 조회를 character use case에 위임한다")
	void delegatesFixedCharacterObjectKeyLookup() {
		given(getCharacterUseCase.findFixedCharacterObjectKey(1L))
				.willReturn(Optional.of("public/characters/fixed/base_image_1.png"));

		Optional<String> result = characterAdapterForUser.findFixedCharacterObjectKey(1L);

		assertThat(result).contains("public/characters/fixed/base_image_1.png");
	}
}
