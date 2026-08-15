package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.adapter.in.web.dto.AiGenerationQuotaResponse;
import com.pikume.back.creative.adapter.in.web.dto.GenerateDiaryImageRequest;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.dto.GenerateDiaryImageCommand;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.QueryAiGenerationQuotaUseCase;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiGeneratorController")
class AiGeneratorControllerTest {

	@Mock
	private QueryAiGenerationQuotaUseCase queryAiGenerationQuotaUseCase;
	@Mock
	private GenerateImageUseCase generateImageUseCase;

	private AiGeneratorController aiGeneratorController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		aiGeneratorController = new AiGeneratorController(
				queryAiGenerationQuotaUseCase,
				generateImageUseCase);
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(aiGeneratorController)
				.setControllerAdvice(
						new GlobalExceptionHandler(Optional.empty(), problemDetailFactory),
						new CreativeExceptionHandler(problemDetailFactory))
				.setValidator(validator)
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver())
				.build();
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 생성 성공 결과를 HTTP 응답으로 변환한다")
	void generateDiaryImageReturnsGeneratedImage() throws Exception {
		given(generateImageUseCase.generateDiaryImage(any(GenerateDiaryImageCommand.class)))
				.willReturn(new GeneratedImageResult(1L, "https://cdn.pikume.com/ai/generated.webp", "ai/generated.webp"));

		ResponseEntity<AiDiaryResponse> response = aiGeneratorController.generateDiaryImage(
				new GenerateDiaryImageRequest("test diary", 7L),
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isInstanceOf(AiDiaryResponse.class);
		then(generateImageUseCase).should().generateDiaryImage(
				new GenerateDiaryImageCommand("test diary", "user1", 7L));
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 캐릭터 식별자 생략을 프로필 참조 명령으로 전달한다")
	void generateDiaryImageKeepsOptionalCharacterIdentifierAbsent() {
		given(generateImageUseCase.generateDiaryImage(any(GenerateDiaryImageCommand.class)))
				.willReturn(new GeneratedImageResult(1L, "https://cdn.pikume.com/ai/generated.webp", "ai/generated.webp"));

		aiGeneratorController.generateDiaryImage(
				new GenerateDiaryImageRequest("test diary", null),
				new UserPrincipal("user1", "pikume"));

		then(generateImageUseCase).should().generateDiaryImage(
				new GenerateDiaryImageCommand("test diary", "user1", null));
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 일일 한도 초과 application 예외를 Web Handler로 전파한다")
	void generateDiaryImagePropagatesRateLimitException() {
		given(generateImageUseCase.generateDiaryImage(any(GenerateDiaryImageCommand.class)))
				.willThrow(new AiGenerationQuotaExceededException(5));

		assertThatThrownBy(() -> aiGeneratorController.generateDiaryImage(
						new GenerateDiaryImageRequest("test diary", null),
						new UserPrincipal("user1", "pikume")))
				.isInstanceOf(AiGenerationQuotaExceededException.class);
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 예상하지 못한 예외를 전역 예외 처리기로 전파한다")
	void generateDiaryImagePropagatesUnexpectedException() throws Exception {
		given(generateImageUseCase.generateDiaryImage(any(GenerateDiaryImageCommand.class)))
				.willThrow(new RuntimeException("internal provider error"));

		assertThatThrownBy(() -> aiGeneratorController.generateDiaryImage(
						new GenerateDiaryImageRequest("test diary", null),
						new UserPrincipal("user1", "pikume")))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("internal provider error");
	}

	@Test
	@DisplayName("GET /api/diary/ai/generate는 남은 AI 생성 횟수를 반환한다")
	void getRemainingRequestsReturnsRemainingGenerationCount() {
		given(queryAiGenerationQuotaUseCase.getRemainingGenerationCount("user1")).willReturn(2);

		ResponseEntity<AiGenerationQuotaResponse> response = aiGeneratorController.getRemainingRequests(
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(new AiGenerationQuotaResponse(2));
	}

	@Test
	@DisplayName("POST /api/diary/ai/generate는 양수가 아닌 캐릭터 식별자를 validation Problem Details로 거절한다")
	void generateDiaryImageRejectsNonPositiveCharacterIdentifier() throws Exception {
		for (long characterId : new long[] {0L, -1L}) {
			mockMvc.perform(post("/api/diary/ai/generate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"test diary","characterId":%d}
								""".formatted(characterId)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.type")
							.value("https://api.pikume.com/problems/validation/invalid-request"))
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.fieldErrors.characterId").exists())
					.andExpect(jsonPath("$.instance").value("/api/diary/ai/generate"));
		}
		then(generateImageUseCase).shouldHaveNoInteractions();
	}

	private static class AuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(UserPrincipal.class);
		}

		@Override
		public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				WebDataBinderFactory binderFactory
		) {
			return new UserPrincipal("user1", "pikume");
		}
	}
}
