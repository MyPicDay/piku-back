package com.pikume.back.creative.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.dto.GeneratedIllustrationPayload;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.policy.DiaryIllustrationPromptPolicy;
import com.pikume.back.creative.application.port.out.CreativeImageStoragePort;
import com.pikume.back.creative.application.port.out.GenerateDiaryIllustrationPort;
import com.pikume.back.creative.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.creative.application.port.out.SaveGenerationPort;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import com.pikume.back.creative.domain.exception.ImageGenerationException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationService")
class ImageGenerationServiceTest {

	@InjectMocks
	private ImageGenerationService imageGenerationService;

	@Mock
	private GenerateDiaryIllustrationPort generateDiaryIllustrationPort;

	@Mock
	private SaveGenerationPort saveGenerationPort;

	@Mock
	private LoadCharacterReferencePort loadCharacterReferencePort;

	@Mock
	private DiaryIllustrationPromptPolicy diaryIllustrationPromptPolicy;

	@Mock
	private CreativeImageStoragePort creativeImageStoragePort;

	@Nested
	@DisplayName("generateDiaryImage")
	class GenerateDiaryImage {

		@Test
		@DisplayName("일기 이미지를 정상 생성한다")
		void generatesDiaryImageSuccessfully() {
			String userId = "user-1";
			String content = "Taking a walk in the park";
			given(loadCharacterReferencePort.findByUserId(userId))
					.willReturn(Optional.of(new CharacterReferenceImage("avatar_path", "base64_avatar")));
			given(diaryIllustrationPromptPolicy.createPrompt(content)).willReturn("generated prompt");
			given(generateDiaryIllustrationPort.generate(any()))
					.willReturn(new GeneratedIllustrationPayload("base64_generated_image", "png"));

			String privateObjectKey = "private/diary-images/ai/ab/cd/generated.png";
			given(creativeImageStoragePort.saveAIPhoto("base64_generated_image", userId, "png")).willReturn(privateObjectKey);
			given(creativeImageStoragePort.getPhotoUrl(privateObjectKey, false)).willReturn("http://url/generated.png");

			given(saveGenerationPort.save(any(DiaryImageGeneration.class))).willAnswer(inv -> {
				DiaryImageGeneration generation = inv.getArgument(0);
				return generation;
			});

			GeneratedImageResult result = imageGenerationService.generateDiaryImage(content, userId);

			assertThat(result.filePath()).isEqualTo(privateObjectKey);
			assertThat(result.imageUrl()).isEqualTo("http://url/generated.png");
			then(saveGenerationPort).should().save(any(DiaryImageGeneration.class));
		}

		@Test
		@DisplayName("참조 캐릭터 이미지를 찾지 못하면 예외가 발생한다")
		void throwsWhenCharacterReferenceMissing() {
			String userId = "user-1";
			given(loadCharacterReferencePort.findByUserId(userId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> imageGenerationService.generateDiaryImage("content", userId))
					.isInstanceOf(ImageGenerationException.class)
					.hasMessageContaining("참조 캐릭터 이미지");
		}
	}
}
