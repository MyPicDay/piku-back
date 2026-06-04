package com.pikume.back.diary.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.pikume.back.diary.application.port.in.CreateDiaryUseCase;
import com.pikume.back.diary.application.port.in.DeleteDiaryUseCase;
import com.pikume.back.diary.application.port.in.GetCalendarUseCase;
import com.pikume.back.diary.application.port.in.UpdateDiaryUseCase;
import com.pikume.back.diary.application.dto.DiaryUpdatedResult;
import com.pikume.back.diary.application.dto.UpdateDiaryCommand;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.diary.application.dto.DiaryGalleryItemView;
import com.pikume.back.diary.application.dto.DiaryGalleryPage;
import com.pikume.back.diary.application.exception.InvalidDiaryGalleryCursorException;
import com.pikume.back.diary.application.port.in.GetDiaryGalleryUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.global.util.RequestMetaMapper;

import java.util.Optional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
	private GetDiaryGalleryUseCase getDiaryGalleryUseCase;

	@Mock
	private UpdateDiaryUseCase updateDiaryUseCase;

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
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		LocalValidatorFactoryBean mvcValidator = new LocalValidatorFactoryBean();
		mvcValidator.afterPropertiesSet();
		diaryController = new DiaryController(
				createDiaryUseCase,
				deleteDiaryUseCase,
				getCalendarUseCase,
				getDiaryGalleryUseCase,
				updateDiaryUseCase,
				fileUtil,
				requestMetaMapper,
				validator,
				problemDetailFactory);
		mockMvc = MockMvcBuilders.standaloneSetup(diaryController)
				.setControllerAdvice(
						new GlobalExceptionHandler(Optional.empty(), problemDetailFactory),
						new DiaryExceptionHandler(problemDetailFactory))
				.setControllerAdvice(
						new GlobalExceptionHandler(Optional.empty(), problemDetailFactory),
						new DiaryExceptionHandler(problemDetailFactory))
				.setValidator(mvcValidator)
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

	@Test
	@DisplayName("GET /api/diary/user/{userId}/gallery는 사용자 사진 갤러리 page shape를 반환한다")
	void getUserDiaryGalleryReturnsCursorPage() throws Exception {
		DiaryGalleryPage<DiaryGalleryItemView> page = new DiaryGalleryPage<>(
				List.of(new DiaryGalleryItemView(
						10L,
						"https://cdn.example/cover.jpg",
						LocalDate.of(2026, 5, 31),
						3L,
						DiaryVisibility.FRIENDS)),
				"opaque-next-cursor",
				true);
		given(getDiaryGalleryUseCase.findGallery("profile-1", "user1", null, 10))
				.willReturn(page);

		mockMvc.perform(get("/api/diary/user/profile-1/gallery")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].diaryId").value(10L))
				.andExpect(jsonPath("$.items[0].coverPhotoUrl").value("https://cdn.example/cover.jpg"))
				.andExpect(jsonPath("$.items[0].date").value("2026-05-31"))
				.andExpect(jsonPath("$.items[0].imageCount").value(3L))
				.andExpect(jsonPath("$.items[0].status").value("FRIENDS"))
				.andExpect(jsonPath("$.nextCursor").value("opaque-next-cursor"))
				.andExpect(jsonPath("$.hasNext").value(true));

		then(getDiaryGalleryUseCase).should()
				.findGallery("profile-1", "user1", null, 10);
	}

	@Test
	@DisplayName("GET /api/diary/user/{userId}/gallery는 limit 10 초과를 validation Problem Details로 거부한다")
	void getUserDiaryGalleryRejectsTooLargeLimit() throws Exception {
		mockMvc.perform(get("/api/diary/user/profile-1/gallery")
						.param("limit", "11")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.limit").exists());

		then(getDiaryGalleryUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /api/diary/user/{userId}/gallery는 잘못된 cursor를 validation Problem Details로 반환한다")
	void getUserDiaryGalleryRejectsInvalidCursorWithProblemDetails() throws Exception {
		given(getDiaryGalleryUseCase.findGallery("profile-1", "user1", "bad", 10))
				.willThrow(new InvalidDiaryGalleryCursorException());

		mockMvc.perform(get("/api/diary/user/profile-1/gallery")
						.param("cursor", "bad")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.cursor").value("유효하지 않은 갤러리 커서입니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary/user/profile-1/gallery"));
	}

	@Test
	@DisplayName("POST /api/diary는 일기 요청 검증 예외를 기존 validation/invalid-request로 처리한다")
	void createDiaryReturnsValidationProblemDetailWhenUseCaseRejectsCommand() throws Exception {
		given(requestMetaMapper.extractMetaInfo(any(HttpServletRequest.class)))
				.willReturn(new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
						"https://localhost:8080/api/diary", "test-agent", "127.0.0.1"));
		willThrow(new DiaryInvalidRequestException("미래 날짜에 일기를 작성할 수 없습니다: 2099-01-01"))
				.given(createDiaryUseCase).createDiary(any(), any(), eq("user1"), any());
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"""
						{"status":"PUBLIC","content":"생성 요청","imageInfos":[],"date":"2099-01-01"}
						""".getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("미래 날짜에 일기를 작성할 수 없습니다: 2099-01-01"))
				.andExpect(jsonPath("$.instance").value("/api/diary"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 JSON body로 일기를 수정하고 결과를 반환한다")
	void updateDiaryReturnsUpdatedResult() throws Exception {
		given(updateDiaryUseCase.updateDiary(
				eq(1L),
				argThat(command -> command != null
						&& command.status() == DiaryVisibility.PUBLIC
						&& command.content().equals("수정 후")),
				eq("user1")))
				.willReturn(new DiaryUpdatedResult(1L, DiaryVisibility.PUBLIC, "수정 후"));

		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"PUBLIC","content":"수정 후"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diaryId").value(1))
				.andExpect(jsonPath("$.status").value("PUBLIC"))
				.andExpect(jsonPath("$.content").value("수정 후"));

		then(updateDiaryUseCase).should().updateDiary(
				eq(1L),
				argThat(command -> command != null
						&& command.status() == DiaryVisibility.PUBLIC
						&& command.content().equals("수정 후")),
				eq("user1"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 DTO에 없는 필드를 무시하고 수정 대상 필드만 전달한다")
	void updateDiaryIgnoresFieldsThatAreNotUpdatable() throws Exception {
		given(updateDiaryUseCase.updateDiary(
				eq(1L),
				argThat(command -> command != null
						&& command.status() == DiaryVisibility.FRIENDS
						&& command.content().equals("수정 후")),
				eq("user1")))
				.willReturn(new DiaryUpdatedResult(1L, DiaryVisibility.FRIENDS, "수정 후"));

		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status":"FRIENDS",
								  "content":"수정 후",
								  "date":"2099-01-01",
								  "imageInfos":[{"type":"USER_IMAGE","order":0,"photoIndex":0}]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diaryId").value(1))
				.andExpect(jsonPath("$.status").value("FRIENDS"))
				.andExpect(jsonPath("$.content").value("수정 후"));

		then(updateDiaryUseCase).should().updateDiary(
				eq(1L),
				argThat(command -> command != null
						&& command.status() == DiaryVisibility.FRIENDS
						&& command.content().equals("수정 후")),
				eq("user1"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 blank content를 validation/invalid-request로 처리한다")
	void updateDiaryRejectsBlankContent() throws Exception {
		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"PUBLIC","content":" "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.content").exists())
				.andExpect(jsonPath("$.instance").value("/api/diary/1"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 null status를 validation/invalid-request로 처리한다")
	void updateDiaryRejectsNullStatus() throws Exception {
		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":null,"content":"수정 후"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.status").exists())
				.andExpect(jsonPath("$.instance").value("/api/diary/1"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 invalid status를 common/malformed-request로 처리한다")
	void updateDiaryRejectsInvalidStatus() throws Exception {
		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"UNKNOWN","content":"수정 후"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/malformed-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 본문을 해석할 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary/1"));
	}

	@Test
	@DisplayName("PATCH /api/diary/{diaryId}는 일기 요청 검증 예외를 diary invalid-request로 처리한다")
	void updateDiaryReturnsDiaryInvalidRequestWhenUseCaseRejectsCommand() throws Exception {
		willThrow(new DiaryInvalidRequestException("일기 수정 요청은 필수입니다."))
				.given(updateDiaryUseCase).updateDiary(eq(1L), any(UpdateDiaryCommand.class), eq("user1"));

		mockMvc.perform(patch("/api/diary/{diaryId}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"PUBLIC","content":"수정 후"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("일기 수정 요청은 필수입니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary/1"));
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
