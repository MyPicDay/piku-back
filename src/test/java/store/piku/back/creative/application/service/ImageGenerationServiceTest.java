package store.piku.back.creative.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import store.piku.back.creative.application.port.out.AiImageGeneratorPort;
import store.piku.back.creative.application.port.out.SaveGenerationPort;
import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.creative.domain.exception.ImageGenerationException;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.file.FileUtil;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.user.domain.User;
import store.piku.back.user._legacy.UserReader;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationService")
class ImageGenerationServiceTest {

	@InjectMocks
	private ImageGenerationService imageGenerationService;

	@Mock
	private AiImageGeneratorPort aiImageGeneratorPort;

	@Mock
	private SaveGenerationPort saveGenerationPort;

	@Mock
	private FileUtil fileUtil;

	@Mock
	private UserReader userReader;

	@Mock
	private PhotoStorageService photoStorage;

	@Nested
	@DisplayName("generateDiaryImage")
	class GenerateDiaryImage {

		@Test
		@DisplayName("일기 이미지를 정상 생성한다")
		void generatesDiaryImageSuccessfully() {
			String userId = "user-1";
			String content = "Taking a walk in the park";
			RequestMetaInfo metaInfo = mock(RequestMetaInfo.class);

			User user = mock(User.class);
			given(user.getAvatar()).willReturn("avatar_path");
			given(userReader.getUserById(userId)).willReturn(user);

			given(fileUtil.getImageAsBase64("avatar_path")).willReturn("base64_avatar");

			given(aiImageGeneratorPort.editImage(eq("base64_avatar"), anyString()))
					.willReturn(Mono.just("base64_generated_image"));

			given(photoStorage.saveAIPhoto("base64_generated_image", userId, "png")).willReturn("user-1/generated.png");
			given(photoStorage.getPhotoUrl("user-1/generated.png", false)).willReturn("http://url/generated.png");

			given(saveGenerationPort.save(any(DiaryImageGeneration.class))).willAnswer(inv -> {
				DiaryImageGeneration generation = inv.getArgument(0);
				return generation;
			});

			DiaryImageGeneration result = imageGenerationService.generateDiaryImage(content, userId, metaInfo);

			assertThat(result.getFilePath()).isEqualTo("user-1/generated.png");
			assertThat(result.getUserId()).isEqualTo(userId);
			then(saveGenerationPort).should().save(any(DiaryImageGeneration.class));
		}

		@Test
		@DisplayName("아바타 로드 실패 시 예외 발생")
		void throwsWhenAvatarLoadFails() {
			String userId = "user-1";
			User user = mock(User.class);
			given(user.getAvatar()).willReturn("invalid_path");
			given(userReader.getUserById(userId)).willReturn(user);
			given(fileUtil.getImageAsBase64("invalid_path")).willReturn(null);

			assertThatThrownBy(
					() -> imageGenerationService.generateDiaryImage("content", userId, mock(RequestMetaInfo.class)))
					.isInstanceOf(ImageGenerationException.class)
					.hasMessageContaining("아바타 이미지");
		}
	}
}
