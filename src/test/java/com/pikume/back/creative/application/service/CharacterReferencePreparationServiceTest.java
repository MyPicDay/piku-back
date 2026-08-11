package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.policy.CharacterReferencePolicy;
import com.pikume.back.creative.application.port.out.LoadReferenceImageObjectPort;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterReferencePreparationService")
class CharacterReferencePreparationServiceTest {

	@Mock
	private LoadUserAvatarReferencePort loadUserAvatarReferencePort;

	@Mock
	private LoadReferenceImageObjectPort loadReferenceImageObjectPort;

	@Test
	@DisplayName("Character가 해석한 고정 캐릭터 참조를 변경하지 않고 Base64 입력으로 준비한다")
	void preparesResolvedFixedCharacterReferenceImage() {
		given(loadUserAvatarReferencePort.loadUserAvatarReference("user-1"))
				.willReturn(Optional.of("public/characters/fixed/base.webp"));
		given(loadReferenceImageObjectPort.loadReferenceImage("public/characters/fixed/base.webp"))
				.willReturn("image".getBytes(StandardCharsets.UTF_8));
		CharacterReferencePreparationService service = service();

		var result = service.prepareCharacterReference("user-1");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().sourcePath()).isEqualTo("public/characters/fixed/base.webp");
		assertThat(result.orElseThrow().imageBase64())
				.isEqualTo(Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("AI 캐릭터 파일명 참조를 고정 캐릭터 경로로 재해석하지 않는다")
	void preservesAiCharacterFilenameReference() {
		given(loadUserAvatarReferencePort.loadUserAvatarReference("user-1"))
				.willReturn(Optional.of("ai_image.png"));
		given(loadReferenceImageObjectPort.loadReferenceImage("ai_image.png"))
				.willReturn("ai-image".getBytes(StandardCharsets.UTF_8));
		CharacterReferencePreparationService service = service();

		var result = service.prepareCharacterReference("user-1");

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().sourcePath()).isEqualTo("ai_image.png");
		assertThat(result.orElseThrow().imageBase64())
				.isEqualTo(Base64.getEncoder().encodeToString("ai-image".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("다운로드 정책이 없는 절대 URL은 Storage 입력으로 사용하지 않는다")
	void rejectsAbsoluteUrlReference() {
		given(loadUserAvatarReferencePort.loadUserAvatarReference("user-1"))
				.willReturn(Optional.of("https://assets.example.com/base.webp"));
		CharacterReferencePreparationService service = service();

		assertThat(service.prepareCharacterReference("user-1")).isEmpty();
		then(loadReferenceImageObjectPort).should(never()).loadReferenceImage(
				org.mockito.ArgumentMatchers.anyString());
	}

	private CharacterReferencePreparationService service() {
		return new CharacterReferencePreparationService(
				loadUserAvatarReferencePort,
				loadReferenceImageObjectPort,
				new CharacterReferencePolicy());
	}
}
