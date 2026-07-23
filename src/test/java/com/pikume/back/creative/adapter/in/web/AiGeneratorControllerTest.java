package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.adapter.in.web.dto.AiGenerationQuotaResponse;
import com.pikume.back.creative.adapter.in.web.dto.GenerateDiaryImageRequest;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.QueryAiGenerationQuotaUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiGeneratorController")
class AiGeneratorControllerTest {

	@Mock
	private QueryAiGenerationQuotaUseCase queryAiGenerationQuotaUseCase;
	@Mock
	private GenerateImageUseCase generateImageUseCase;

	private AiGeneratorController aiGeneratorController;

	@BeforeEach
	void setUp() {
		aiGeneratorController = new AiGeneratorController(
				queryAiGenerationQuotaUseCase,
				generateImageUseCase);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 생성 성공 결과를 HTTP 응답으로 변환한다")
	void generateDiaryImageReturnsGeneratedImage() throws Exception {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willReturn(new GeneratedImageResult(1L, "https://cdn.pikume.com/ai/generated.webp", "ai/generated.webp"));

		ResponseEntity<AiDiaryResponse> response = aiGeneratorController.generateDiaryImage(
				new GenerateDiaryImageRequest("test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isInstanceOf(AiDiaryResponse.class);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 일일 한도 초과 application 예외를 Web Handler로 전파한다")
	void generateDiaryImagePropagatesRateLimitException() {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willThrow(new AiGenerationQuotaExceededException(5));

		assertThatThrownBy(() -> aiGeneratorController.generateDiaryImage(
						new GenerateDiaryImageRequest("test diary"),
						new CustomUserDetails("user1", "pikume")))
				.isInstanceOf(AiGenerationQuotaExceededException.class);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 예상하지 못한 예외를 전역 예외 처리기로 전파한다")
	void generateDiaryImagePropagatesUnexpectedException() throws Exception {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willThrow(new RuntimeException("internal provider error"));

		assertThatThrownBy(() -> aiGeneratorController.generateDiaryImage(
						new GenerateDiaryImageRequest("test diary"),
						new CustomUserDetails("user1", "pikume")))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("internal provider error");
	}

	@Test
	@DisplayName("GET /api/diary/ai/generate는 남은 AI 생성 횟수를 반환한다")
	void getRemainingRequestsReturnsRemainingGenerationCount() {
		given(queryAiGenerationQuotaUseCase.getRemainingGenerationCount("user1")).willReturn(2);

		ResponseEntity<AiGenerationQuotaResponse> response = aiGeneratorController.getRemainingRequests(
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(new AiGenerationQuotaResponse(2));
	}
}
