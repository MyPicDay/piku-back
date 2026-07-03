package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.ManageAiGenerationQuotaUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiGeneratorController")
class AiGeneratorControllerTest {

	@Mock
	private ManageAiGenerationQuotaUseCase manageAiGenerationQuotaUseCase;
	@Mock
	private GenerateImageUseCase generateImageUseCase;

	private AiGeneratorController aiGeneratorController;

	@BeforeEach
	void setUp() {
		aiGeneratorController = new AiGeneratorController(
				manageAiGenerationQuotaUseCase,
				generateImageUseCase,
				new ProblemDetailFactory());
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 생성 성공 결과를 HTTP 응답으로 변환한다")
	void generateDiaryImageReturnsGeneratedImage() throws Exception {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willReturn(new GeneratedImageResult(1L, "https://cdn.pikume.com/ai/generated.webp", "ai/generated.webp"));

		ResponseEntity<Object> response = aiGeneratorController.generateDiaryImage(
				Map.of("content", "test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isInstanceOf(AiDiaryResponse.class);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 일일 한도 초과 application 예외를 Problem Details로 변환한다")
	void generateDiaryImageReturnsProblemDetailWhenRateLimited() throws Exception {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willThrow(new AiGenerationQuotaExceededException(5));

		ResponseEntity<Object> response = aiGeneratorController.generateDiaryImage(
				Map.of("content", "test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(429);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/rate-limit-exceeded");
		assertThat(problemDetail.getStatus()).isEqualTo(429);
		assertThat(problemDetail.getDetail()).isEqualTo("일일 생성 횟수(5회)를 모두 사용하셨습니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/diary/ai/generate");
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 예상하지 못한 예외를 전역 예외 처리기로 전파한다")
	void generateDiaryImagePropagatesUnexpectedException() throws Exception {
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willThrow(new RuntimeException("internal provider error"));

		assertThatThrownBy(() -> aiGeneratorController.generateDiaryImage(
						Map.of("content", "test diary"),
						new CustomUserDetails("user1", "pikume")))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("internal provider error");
	}

	@Test
	@DisplayName("GET /api/diary/ai/generate는 남은 AI 생성 횟수를 반환한다")
	void getRemainingRequestsReturnsRemainingGenerationCount() {
		given(manageAiGenerationQuotaUseCase.getRemainingGenerationCount("user1")).willReturn(2);

		ResponseEntity<Map<String, Integer>> response = aiGeneratorController.getRemainingRequests(
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).containsEntry("remainingRequests", 2);
	}
}
