package com.pikume.back.character.application.service;

import com.pikume.back.character.application.port.out.CanonicalizeFixedCharacterObjectKeyPort;
import com.pikume.back.character.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.character.domain.Character;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsableCharacterReferenceService")
class UsableCharacterReferenceServiceTest {

	@InjectMocks
	private UsableCharacterReferenceService service;

	@Mock
	private LoadCharacterReferencePort loadCharacterReferencePort;

	@Mock
	private CanonicalizeFixedCharacterObjectKeyPort canonicalizeFixedCharacterObjectKeyPort;

	@Test
	@DisplayName("고정 캐릭터를 모든 요청 사용자에게 canonical 저장 참조로 제공한다")
	void resolvesFixedCharacterForEveryUser() {
		given(loadCharacterReferencePort.loadCharacterReference(1L))
				.willReturn(Optional.of(Character.fixed("base.webp")));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("base.webp"))
				.willReturn("public/characters/fixed/base.webp");

		assertThat(service.resolveUsableReference("user-1", 1L))
				.hasValueSatisfying(result -> assertThat(result.storageReference())
						.isEqualTo("public/characters/fixed/base.webp"));
	}

	@Test
	@DisplayName("AI 생성 캐릭터를 소유 사용자에게만 저장 참조로 제공한다")
	void resolvesAiGeneratedCharacterOnlyForOwner() {
		Character character = Character.aiGenerated("owner-1", "private/characters/generated.webp");
		given(loadCharacterReferencePort.loadCharacterReference(2L))
				.willReturn(Optional.of(character));

		assertThat(service.resolveUsableReference("owner-1", 2L))
				.hasValueSatisfying(result -> assertThat(result.storageReference())
						.isEqualTo("private/characters/generated.webp"));
		assertThat(service.resolveUsableReference("user-2", 2L)).isEmpty();
		then(canonicalizeFixedCharacterObjectKeyPort).should(never())
				.canonicalizeFixedCharacterObjectKey(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	@DisplayName("존재하지 않거나 안전하지 않은 캐릭터 참조는 공개하지 않는다")
	void returnsEmptyForMissingOrUnsafeReference() {
		given(loadCharacterReferencePort.loadCharacterReference(3L)).willReturn(Optional.empty());
		given(loadCharacterReferencePort.loadCharacterReference(4L))
				.willReturn(Optional.of(Character.aiGenerated("owner-1", "https://assets.example.com/character.webp")));

		assertThat(service.resolveUsableReference("owner-1", 3L)).isEmpty();
		assertThat(service.resolveUsableReference("owner-1", 4L)).isEmpty();
	}

	@Test
	@DisplayName("Character 저장 기술 장애를 사용 불가 결과로 축소하지 않는다")
	void propagatesUnexpectedStorageFailure() {
		IllegalStateException failure = new IllegalStateException("database unavailable");
		given(loadCharacterReferencePort.loadCharacterReference(1L)).willThrow(failure);

		assertThatThrownBy(() -> service.resolveUsableReference("user-1", 1L))
				.isSameAs(failure);
	}

	@Test
	@DisplayName("잘못된 고정 캐릭터 저장 참조는 사용 불가로 처리한다")
	void returnsEmptyForInvalidFixedReference() {
		given(loadCharacterReferencePort.loadCharacterReference(1L))
				.willReturn(Optional.of(Character.fixed("invalid.webp")));
		given(canonicalizeFixedCharacterObjectKeyPort.canonicalizeFixedCharacterObjectKey("invalid.webp"))
				.willThrow(new IllegalArgumentException("invalid reference"));

		assertThat(service.resolveUsableReference("user-1", 1L)).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	@DisplayName("저장소에서 복원된 AI 캐릭터의 잘못된 이미지 참조는 사용 불가로 처리한다")
	void returnsEmptyForInvalidPersistedAiGeneratedReference(String storedReference) {
		Character character = Character.aiGenerated("owner-1", "private/characters/generated.webp");
		ReflectionTestUtils.setField(character, "imageReference", storedReference);
		given(loadCharacterReferencePort.loadCharacterReference(2L))
				.willReturn(Optional.of(character));

		assertThat(service.resolveUsableReference("owner-1", 2L)).isEmpty();
	}
}
