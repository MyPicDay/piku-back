package com.pikume.back.user.application.service;

import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.exception.UserAvatarReferenceIntegrityException;
import com.pikume.back.user.application.port.out.ResolveAvatarCharacterReferencesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarReferenceResolver")
class UserAvatarReferenceResolverTest {

	@InjectMocks
	private UserAvatarReferenceResolver resolver;

	@Mock
	private ResolveAvatarCharacterReferencesPort resolveAvatarCharacterReferencesPort;

	@Test
	@DisplayName("사용자와 캐릭터 선택 쌍을 한 번에 해석해 Character가 결정한 참조를 반환한다")
	void resolvesDistinctUserCharacterSelectionsInBatch() {
		AvatarCharacterSelection first = new AvatarCharacterSelection("user-1", 1L);
		AvatarCharacterSelection second = new AvatarCharacterSelection("user-2", 1L);
		given(resolveAvatarCharacterReferencesPort.resolveAvatarCharacterReferences(Set.of(first, second)))
				.willReturn(List.of(
						new AvatarCharacterReference(
								"user-2", 1L,
								new UserAvatarReference("public/characters/fixed/base.webp", false, true)),
						new AvatarCharacterReference(
								"user-1", 1L,
								new UserAvatarReference("public/characters/fixed/base.webp", false, true))));

		Map<AvatarCharacterSelection, UserAvatarReference> result = resolver.resolveRequired(
				List.of(first, second, first));

		assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
				first, new UserAvatarReference("public/characters/fixed/base.webp", false, true),
				second, new UserAvatarReference("public/characters/fixed/base.webp", false, true)));
	}

	@Test
	@DisplayName("요청한 캐릭터 참조가 하나라도 누락되면 데이터 정합성 오류로 실패한다")
	void failsWhenAnyRequiredCharacterReferenceIsMissing() {
		AvatarCharacterSelection first = new AvatarCharacterSelection("user-1", 1L);
		AvatarCharacterSelection inaccessibleAi = new AvatarCharacterSelection("user-2", 2L);
		given(resolveAvatarCharacterReferencesPort.resolveAvatarCharacterReferences(
				Set.of(first, inaccessibleAi)))
				.willReturn(List.of(
						new AvatarCharacterReference(
								"user-1", 1L,
								new UserAvatarReference("public/characters/fixed/base.webp", false, true))));

		assertThatThrownBy(() -> resolver.resolveRequired(List.of(first, inaccessibleAi)))
				.isInstanceOf(UserAvatarReferenceIntegrityException.class)
				.hasMessageContaining("2");
	}

	@Test
	@DisplayName("빈 입력은 외부 컨텍스트를 호출하지 않고 빈 매핑을 반환한다")
	void returnsEmptyWithoutCrossContextCall() {
		assertThat(resolver.resolveRequired(List.of())).isEmpty();

		then(resolveAvatarCharacterReferencesPort).should(never())
				.resolveAvatarCharacterReferences(Set.of());
	}
}
