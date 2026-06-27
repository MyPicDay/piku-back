package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.RecordAiPhotoStatisticsUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.service.RedisService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiGeneratorController")
class AiGeneratorControllerTest {

	@Mock
	private RedisService redisService;

	@Mock
	private GenerateImageUseCase generateImageUseCase;
	@Mock
	private RecordAiPhotoStatisticsUseCase recordAiPhotoStatisticsUseCase;

	private AiGeneratorController aiGeneratorController;

	@BeforeEach
	void setUp() {
		aiGeneratorController = new AiGeneratorController(
				redisService,
				generateImageUseCase,
				new ProblemDetailFactory(),
				recordAiPhotoStatisticsUseCase);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 생성 성공 시 요청과 성공 통계를 기록한다")
	void generateDiaryImageRecordsRequestAndSuccessStatistics() throws Exception {
		given(redisService.isLimitExceeded(anyString(), anyString(), anyInt()))
				.willReturn(false);
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willReturn(new GeneratedImageResult(1L, "https://cdn.pikume.com/ai/generated.webp", "ai/generated.webp"));

		ResponseEntity<Object> response = aiGeneratorController.generateDiaryImage(
				Map.of("content", "test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isInstanceOf(AiDiaryResponse.class);
		then(recordAiPhotoStatisticsUseCase).should().recordRequest("user1");
		then(recordAiPhotoStatisticsUseCase).should().recordSuccess("user1");
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 일일 한도 초과 시 Problem Details를 반환한다")
	void generateDiaryImageReturnsProblemDetailWhenRateLimited() throws Exception {
		given(redisService.isLimitExceeded(anyString(), anyString(), anyInt()))
				.willReturn(true);

		ResponseEntity<Object> response = aiGeneratorController.generateDiaryImage(
				Map.of("content", "test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(429);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/rate-limit-exceeded");
		assertThat(problemDetail.getStatus()).isEqualTo(429);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/diary/ai/generate");
		then(recordAiPhotoStatisticsUseCase).should().recordRequest("user1");
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 생성 실패 시 Problem Details를 반환한다")
	void generateDiaryImageReturnsProblemDetailWhenGenerationFails() throws Exception {
		given(redisService.isLimitExceeded(anyString(), anyString(), anyInt()))
				.willReturn(false);
		given(generateImageUseCase.generateDiaryImage(anyString(), anyString()))
				.willThrow(new RuntimeException("AI 이미지 생성에 실패했습니다."));

		ResponseEntity<Object> response = aiGeneratorController.generateDiaryImage(
				Map.of("content", "test diary"),
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/internal-server-error");
		assertThat(problemDetail.getStatus()).isEqualTo(500);
		assertThat(problemDetail.getDetail()).isEqualTo("AI 이미지 생성에 실패했습니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/diary/ai/generate");
		then(recordAiPhotoStatisticsUseCase).should().recordRequest("user1");
		then(recordAiPhotoStatisticsUseCase).should().recordFailure("user1");
	}
}
