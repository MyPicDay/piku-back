package com.pikume.back.diary.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.pikume.back.diary.application.port.in.CreateDiaryUseCase;
import com.pikume.back.diary.application.port.in.DeleteDiaryUseCase;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.global.util.RequestMetaMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryController")
class DiaryControllerTest {

	@Mock
	private CreateDiaryUseCase createDiaryUseCase;

	@Mock
	private DeleteDiaryUseCase deleteDiaryUseCase;

	@Mock
	private GetCalendarUseCase getCalendarUseCase;

	@Mock
	private FileUtil fileUtil;

	@Mock
	private RequestMetaMapper requestMetaMapper;

	@Mock
	private jakarta.validation.Validator validator;

	private DiaryController diaryController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		diaryController = new DiaryController(
				createDiaryUseCase,
				deleteDiaryUseCase,
				getCalendarUseCase,
				fileUtil,
				requestMetaMapper,
				validator,
				new ProblemDetailFactory());
		mockMvc = MockMvcBuilders.standaloneSetup(diaryController)
				.setControllerAdvice(new GlobalExceptionHandler(Optional.empty(), new ProblemDetailFactory()))
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver())
				.build();
	}

	@Test
	@DisplayName("GET /api/diary/images/{userId}/{filename}는 파일이 없으면 Problem Details를 반환한다")
	void getFileReturnsProblemDetailWhenImageDoesNotExist() {
		willThrow(new RuntimeException("missing")).given(fileUtil).loadFileAsResource("user1/missing.png");

		ResponseEntity<?> response = diaryController.getFile("user1", "missing.png");

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/diary/images/user1/missing.png");
	}

	@Test
	@DisplayName("POST /api/diary는 malformed body를 전역 common/malformed-request로 처리한다")
	void createDiaryReturnsMalformedRequestProblemDetailFromGlobalHandler() throws Exception {
		MockMultipartFile invalidDiary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"{invalid".getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(invalidDiary))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/malformed-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 본문을 해석할 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary"));
	}

	private static class AuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(CustomUserDetails.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return new CustomUserDetails("user1", "user@example.com", "pikume");
		}
	}
}
