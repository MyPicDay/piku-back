package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.character.application.dto.CharacterImageReferenceResult;
import com.pikume.back.character.application.dto.CharacterImageReferenceQuery;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.application.port.in.QueryCharacterImageReferencesUseCase;
import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

	@Mock
	private QueryCharacterImageReferencesUseCase queryCharacterImageReferencesUseCase;

	@Test
	@DisplayName("사용자 프로필용 고정 캐릭터 object key 조회를 character 공개 use case에 위임한다")
	void delegatesFixedCharacterObjectKeyLookup() {
		given(getCharacterUseCase.findFixedCharacterObjectKey(1L))
				.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));

		Optional<String> result = characterAdapterForUser.resolveFixedCharacterObjectKey(1L);

		assertThat(result).contains("public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("사용자 아바타 참조 일괄 조회를 Character 공개 use case에 위임한다")
	void delegatesAvatarCharacterReferenceBatchLookup() {
		List<AvatarCharacterSelection> selections = List.of(
				new AvatarCharacterSelection("user-1", 1L),
				new AvatarCharacterSelection("user-1", 2L));
		given(queryCharacterImageReferencesUseCase.queryCharacterImageReferences(List.of(
				new CharacterImageReferenceQuery("user-1", 1L),
				new CharacterImageReferenceQuery("user-1", 2L))))
				.willReturn(List.of(
						new CharacterImageReferenceResult(
								"user-1", 1L, "public/characters/fixed/base.webp", false, true),
						new CharacterImageReferenceResult(
								"user-1", 2L, "generated.webp", false, false)));

		List<AvatarCharacterReference> result = characterAdapterForUser
				.resolveAvatarCharacterReferences(selections);

		assertThat(result).containsExactly(
				new AvatarCharacterReference(
						"user-1", 1L,
						new UserAvatarReference("public/characters/fixed/base.webp", false, true)),
				new AvatarCharacterReference(
						"user-1", 2L,
						new UserAvatarReference("generated.webp", false, false)));
	}
}
