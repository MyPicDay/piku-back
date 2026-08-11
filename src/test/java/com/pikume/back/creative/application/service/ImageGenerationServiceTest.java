package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;
import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.dto.GeneratedIllustrationPayload;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.dto.GenerateDiaryImageCommand;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.creative.application.policy.DiaryIllustrationPromptPolicy;
import com.pikume.back.creative.application.port.in.ConsumeAiGenerationQuotaUseCase;
import com.pikume.back.creative.application.port.in.PrepareCharacterReferenceUseCase;
import com.pikume.back.creative.application.port.in.RecordAiPhotoStatisticsUseCase;
import com.pikume.back.creative.application.port.out.CreativeImageStoragePort;
import com.pikume.back.creative.application.port.out.GenerateDiaryIllustrationPort;
import com.pikume.back.creative.application.port.out.RecordGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationService")
class ImageGenerationServiceTest {

	@InjectMocks
	private ImageGenerationService imageGenerationService;

	@Mock
	private GenerateDiaryIllustrationPort generateDiaryIllustrationPort;
	@Mock
	private RecordGenerationPort recordGenerationPort;
	@Mock
	private PrepareCharacterReferenceUseCase prepareCharacterReferenceUseCase;
	@Mock
	private DiaryIllustrationPromptPolicy diaryIllustrationPromptPolicy;
	@Mock
	private CreativeImageStoragePort creativeImageStoragePort;
	@Mock
	private ConsumeAiGenerationQuotaUseCase consumeAiGenerationQuotaUseCase;
	@Mock
	private RecordAiPhotoStatisticsUseCase recordAiPhotoStatisticsUseCase;

	@Nested
	@DisplayName("generateDiaryImage")
	class GenerateDiaryImage {

		@Test
		@DisplayName("일기 이미지를 정상 생성하면 선차감한 사용량을 확정 사용량으로 둔다")
		void generatesDiaryImageSuccessfully() {
			String userId = "user-1";
			String content = "Taking a walk in the park";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(true, 3, 2));
			given(prepareCharacterReferenceUseCase.prepareCharacterReference(userId, null))
					.willReturn(Optional.of(new CharacterReferenceImage("avatar_path", "base64_avatar")));
			given(diaryIllustrationPromptPolicy.createPrompt(content)).willReturn("generated prompt");
			given(generateDiaryIllustrationPort.generate(any()))
					.willReturn(new GeneratedIllustrationPayload("base64_generated_image", "png"));

			String privateObjectKey = "private/diary-images/ai/ab/cd/generated.png";
			given(creativeImageStoragePort.storeGeneratedImage("base64_generated_image", userId, "png"))
					.willReturn(privateObjectKey);
			given(creativeImageStoragePort.resolveGeneratedImageUrl(privateObjectKey, false))
					.willReturn("http://url/generated.png");

			given(recordGenerationPort.recordGeneration(any(DiaryImageGeneration.class)))
					.willAnswer(inv -> inv.getArgument(0));

			GeneratedImageResult result = imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand(content, userId, null));

			assertThat(result.filePath()).isEqualTo(privateObjectKey);
			assertThat(result.imageUrl()).isEqualTo("http://url/generated.png");
			then(recordGenerationPort).should().recordGeneration(any(DiaryImageGeneration.class));
			then(consumeAiGenerationQuotaUseCase).should(never()).releaseGenerationConsumption(userId);

			InOrder inOrder = inOrder(
					recordAiPhotoStatisticsUseCase,
					consumeAiGenerationQuotaUseCase,
					generateDiaryIllustrationPort);
			inOrder.verify(recordAiPhotoStatisticsUseCase).recordRequest(userId);
			inOrder.verify(consumeAiGenerationQuotaUseCase).tryConsumeForGeneration(userId);
			inOrder.verify(generateDiaryIllustrationPort).generate(any());
			then(recordAiPhotoStatisticsUseCase).should().recordSuccess(userId);
		}

		@Test
		@DisplayName("일일 한도에 도달하면 AI 생성 외부 포트를 호출하지 않는다")
		void throwsWhenQuotaConsumptionIsRejected() {
			String userId = "user-1";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(false, 3, 0));

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand("content", userId, null)))
					.isInstanceOf(AiGenerationQuotaExceededException.class)
					.hasMessageContaining("일일 생성 횟수");

			then(recordAiPhotoStatisticsUseCase).should().recordRequest(userId);
			then(generateDiaryIllustrationPort).should(never()).generate(any());
			then(creativeImageStoragePort).should(never()).storeGeneratedImage(any(), any(), any());
			then(recordGenerationPort).should(never()).recordGeneration(any());
			then(recordAiPhotoStatisticsUseCase).should(never()).recordFailure(userId);
		}

		@Test
		@DisplayName("참조 캐릭터 이미지를 찾지 못하면 선차감한 사용량을 취소한다")
		void releasesConsumptionWhenCharacterReferenceMissing() {
			String userId = "user-1";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(true, 3, 2));
			given(prepareCharacterReferenceUseCase.prepareCharacterReference(userId, null)).willReturn(Optional.empty());

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand("content", userId, null)))
					.isInstanceOf(CreativeException.class)
					.hasMessageContaining("참조 캐릭터 이미지");

			then(consumeAiGenerationQuotaUseCase).should().releaseGenerationConsumption(userId);
			then(recordAiPhotoStatisticsUseCase).should().recordFailure(userId);
			then(generateDiaryIllustrationPort).should(never()).generate(any());
		}

		@Test
		@DisplayName("외부 AI 이미지 생성 실패 시 선차감한 사용량을 취소한다")
		void releasesConsumptionWhenIllustrationGenerationFails() {
			String userId = "user-1";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(true, 3, 2));
			given(prepareCharacterReferenceUseCase.prepareCharacterReference(userId, null))
					.willReturn(Optional.of(new CharacterReferenceImage("avatar_path", "base64_avatar")));
			given(diaryIllustrationPromptPolicy.createPrompt("content")).willReturn("generated prompt");
			given(generateDiaryIllustrationPort.generate(any()))
					.willThrow(new CreativeException(CreativeErrorCode.IMAGE_GENERATION_FAILED));

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand("content", userId, null)))
					.isInstanceOf(CreativeException.class)
					.hasMessageContaining("AI 이미지 생성에 실패했습니다.");

			then(consumeAiGenerationQuotaUseCase).should().releaseGenerationConsumption(userId);
			then(recordAiPhotoStatisticsUseCase).should().recordFailure(userId);
			then(recordGenerationPort).should(never()).recordGeneration(any());
		}

		@Test
		@DisplayName("이미지 저장 실패 시 선차감한 사용량을 취소한다")
		void releasesConsumptionWhenStorageFails() {
			String userId = "user-1";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(true, 3, 2));
			given(prepareCharacterReferenceUseCase.prepareCharacterReference(userId, null))
					.willReturn(Optional.of(new CharacterReferenceImage("avatar_path", "base64_avatar")));
			given(diaryIllustrationPromptPolicy.createPrompt("content")).willReturn("generated prompt");
			given(generateDiaryIllustrationPort.generate(any()))
					.willReturn(new GeneratedIllustrationPayload("base64_generated_image", "png"));
			given(creativeImageStoragePort.storeGeneratedImage("base64_generated_image", userId, "png"))
					.willThrow(new CreativeException(CreativeErrorCode.IMAGE_STORAGE_FAILED));

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand("content", userId, null)))
					.isInstanceOf(CreativeException.class)
					.hasMessageContaining("저장");

			then(consumeAiGenerationQuotaUseCase).should().releaseGenerationConsumption(userId);
			then(recordAiPhotoStatisticsUseCase).should().recordFailure(userId);
			then(recordGenerationPort).should(never()).recordGeneration(any());
		}

		@Test
		@DisplayName("선택 캐릭터 사용 불가 시 선차감과 실패 통계를 보상하고 외부 생성을 호출하지 않는다")
		void compensatesWhenSelectedCharacterIsUnavailable() {
			String userId = "user-1";
			given(consumeAiGenerationQuotaUseCase.tryConsumeForGeneration(userId))
					.willReturn(new AiGenerationQuotaConsumption(true, 3, 2));
			given(prepareCharacterReferenceUseCase.prepareCharacterReference(userId, 7L))
					.willThrow(new CreativeException(CreativeErrorCode.SELECTED_CHARACTER_UNAVAILABLE));

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage(
					new GenerateDiaryImageCommand("content", userId, 7L)))
					.isInstanceOf(CreativeException.class)
					.hasMessage("선택한 캐릭터를 사용할 수 없습니다.");

			then(consumeAiGenerationQuotaUseCase).should().releaseGenerationConsumption(userId);
			then(recordAiPhotoStatisticsUseCase).should().recordFailure(userId);
			then(generateDiaryIllustrationPort).shouldHaveNoInteractions();
			then(creativeImageStoragePort).shouldHaveNoInteractions();
			then(recordGenerationPort).shouldHaveNoInteractions();
		}
	}
}
