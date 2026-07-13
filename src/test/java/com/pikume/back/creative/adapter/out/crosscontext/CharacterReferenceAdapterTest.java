package com.pikume.back.creative.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.port.out.LoadObjectPort;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterReferenceAdapter")
class CharacterReferenceAdapterTest {

	@InjectMocks
	private CharacterReferenceAdapter characterReferenceAdapter;

	@Mock
	private QueryUserSummaryUseCase queryUserSummaryUseCase;

	@Mock
	private LoadObjectPort loadObjectPort;

	@Test
	@DisplayName("사용자 고정 캐릭터 아바타 object를 Base64 creative 참조 이미지로 변환한다")
	void returnsCharacterReferenceImage() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of("user-1", userSummary("public/characters/fixed/base_image_1.webp")));
		given(loadObjectPort.loadObject("public/characters/fixed/base_image_1.webp"))
				.willReturn("fixed-character".getBytes(StandardCharsets.UTF_8));

		var result = characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");

		assertThat(result).isPresent();
		assertThat(result.get().sourcePath()).isEqualTo("public/characters/fixed/base_image_1.webp");
		assertThat(result.get().imageBase64())
				.isEqualTo(Base64.getEncoder().encodeToString("fixed-character".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("legacy 고정 캐릭터 아바타 path는 canonical object key로 읽는다")
	void normalizesLegacyFixedCharacterAvatarPath() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of("user-1", userSummary("characters/fixed/base_image_1.webp")));
		given(loadObjectPort.loadObject("public/characters/fixed/base_image_1.webp"))
				.willReturn("fixed-character".getBytes(StandardCharsets.UTF_8));

		var result = characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");

		assertThat(result).isPresent();
		assertThat(result.get().sourcePath()).isEqualTo("public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("같은 fixed character object key는 한 번만 storage에서 읽고 캐시한다")
	void cachesFixedCharacterReferenceImage() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of("user-1", userSummary("public/characters/fixed/base_image_1.webp")));
		given(loadObjectPort.loadObject("public/characters/fixed/base_image_1.webp"))
				.willReturn("fixed-character".getBytes(StandardCharsets.UTF_8));

		characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");
		characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");

		verify(loadObjectPort, times(1)).loadObject("public/characters/fixed/base_image_1.webp");
	}

	@Test
	@DisplayName("아바타 이미지를 읽지 못하면 빈 결과를 반환한다")
	void returnsEmptyWhenAvatarCannotBeRead() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of("user-1", userSummary("public/characters/fixed/missing.png")));
		given(loadObjectPort.loadObject("public/characters/fixed/missing.png"))
				.willThrow(new RuntimeException("missing"));

		var result = characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("사용자 아바타 경로가 유효하지 않으면 storage를 호출하지 않고 빈 결과를 반환한다")
	void returnsEmptyWhenAvatarPathIsInvalid() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of("user-1", userSummary("characters/fixed/group/../bad.png")));

		var result = characterReferenceAdapter.loadCharacterReferenceForGeneration("user-1");

		assertThat(result).isEmpty();
		verifyNoInteractions(loadObjectPort);
	}

	private UserSummaryView userSummary(String avatarPath) {
		return new UserSummaryView("user-1", "피쿠", avatarPath);
	}
}
