package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.policy.CharacterReferencePolicy;
import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.creative.application.port.out.LoadReferenceImageObjectPort;
import com.pikume.back.creative.application.port.out.LoadSelectedCharacterReferencePort;
import com.pikume.back.creative.application.port.out.LoadUserAvatarReferencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterReferencePreparationService")
class CharacterReferencePreparationServiceTest {

	@Mock
	private LoadUserAvatarReferencePort loadUserAvatarReferencePort;

	@Mock
	private LoadSelectedCharacterReferencePort loadSelectedCharacterReferencePort;

	@Mock
	private LoadReferenceImageObjectPort loadReferenceImageObjectPort;

	@Test
	@DisplayName("legacy 고정 캐릭터 참조를 canonical Object Key와 Base64 입력으로 준비한다")
	void preparesCanonicalReferenceImage() {
		given(loadUserAvatarReferencePort.loadUserAvatarReference("user-1"))
				.willReturn(Optional.of("characters/fixed/base.webp"));
		given(loadReferenceImageObjectPort.loadReferenceImage("public/characters/fixed/base.webp"))
				.willReturn("image".getBytes(StandardCharsets.UTF_8));
		CharacterReferencePreparationService service = service();

		var result = service.prepareCharacterReference("user-1", null);

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().sourcePath()).isEqualTo("public/characters/fixed/base.webp");
		assertThat(result.orElseThrow().imageBase64())
				.isEqualTo(Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8)));
		then(loadSelectedCharacterReferencePort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("다운로드 정책이 없는 절대 URL은 Storage 입력으로 사용하지 않는다")
	void rejectsAbsoluteUrlReference() {
		given(loadUserAvatarReferencePort.loadUserAvatarReference("user-1"))
				.willReturn(Optional.of("https://assets.example.com/base.webp"));
		CharacterReferencePreparationService service = service();

		assertThat(service.prepareCharacterReference("user-1", null)).isEmpty();
		then(loadReferenceImageObjectPort).should(never()).loadReferenceImage(
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	@DisplayName("캐릭터 식별자가 있으면 선택 캐릭터 참조만 준비한다")
	void preparesOnlySelectedCharacterReference() {
		given(loadSelectedCharacterReferencePort.loadSelectedCharacterReference("user-1", 7L))
				.willReturn(Optional.of("private/characters/user-1/generated.webp"));
		given(loadReferenceImageObjectPort.loadReferenceImage("private/characters/user-1/generated.webp"))
				.willReturn("selected".getBytes(StandardCharsets.UTF_8));
		CharacterReferencePreparationService service = service();

		var result = service.prepareCharacterReference("user-1", 7L);

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().sourcePath())
				.isEqualTo("private/characters/user-1/generated.webp");
		then(loadUserAvatarReferencePort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("명시한 캐릭터를 사용할 수 없으면 프로필 아바타로 폴백하지 않는다")
	void rejectsUnavailableSelectedCharacterWithoutAvatarFallback() {
		given(loadSelectedCharacterReferencePort.loadSelectedCharacterReference("user-1", 7L))
				.willReturn(Optional.empty());
		CharacterReferencePreparationService service = service();

		assertThatThrownBy(() -> service.prepareCharacterReference("user-1", 7L))
				.isInstanceOf(CreativeException.class)
				.extracting(exception -> ((CreativeException) exception).getErrorCode())
				.isEqualTo(CreativeErrorCode.SELECTED_CHARACTER_UNAVAILABLE);
		then(loadUserAvatarReferencePort).shouldHaveNoInteractions();
		then(loadReferenceImageObjectPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("Character 조회 기술 장애를 빈 참조로 축소하지 않는다")
	void propagatesSelectedCharacterLookupFailure() {
		IllegalStateException failure = new IllegalStateException("database unavailable");
		given(loadSelectedCharacterReferencePort.loadSelectedCharacterReference("user-1", 7L))
				.willThrow(failure);

		assertThatThrownBy(() -> service().prepareCharacterReference("user-1", 7L))
				.isSameAs(failure);
	}

	@Test
	@DisplayName("Object Storage 기술 장애를 빈 참조로 축소하지 않는다")
	void propagatesReferenceImageStorageFailure() {
		given(loadSelectedCharacterReferencePort.loadSelectedCharacterReference("user-1", 7L))
				.willReturn(Optional.of("private/characters/user-1/generated.webp"));
		IllegalStateException failure = new IllegalStateException("storage unavailable");
		given(loadReferenceImageObjectPort.loadReferenceImage("private/characters/user-1/generated.webp"))
				.willThrow(failure);

		assertThatThrownBy(() -> service().prepareCharacterReference("user-1", 7L))
				.isSameAs(failure);
	}

	private CharacterReferencePreparationService service() {
		return new CharacterReferencePreparationService(
				loadUserAvatarReferencePort,
				loadSelectedCharacterReferencePort,
				loadReferenceImageObjectPort,
				new CharacterReferencePolicy());
	}
}
