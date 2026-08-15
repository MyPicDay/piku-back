package com.pikume.back.diary.adapter.in.web;

import com.pikume.back.diary.adapter.in.web.dto.DiaryDTO;
import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryCreatedResult;
import com.pikume.back.diary.application.dto.DiaryGalleryItemView;
import com.pikume.back.diary.application.dto.DiaryGalleryPage;
import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import com.pikume.back.diary.application.dto.DiaryUpdatedResult;
import com.pikume.back.diary.application.dto.UpdateDiaryCommand;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.exception.DuplicateDiaryException;
import com.pikume.back.diary.application.exception.InvalidDiaryGalleryCursorException;
import com.pikume.back.diary.application.port.in.*;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
	private UpdateDiaryUseCase updateDiaryUseCase;

	@Mock
	private GetDiaryGalleryUseCase getDiaryGalleryUseCase;

	private DiaryController diaryController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ProblemDetailFactory problemDetailFactory = new ProblemDetailFactory();
		LocalValidatorFactoryBean mvcValidator = new LocalValidatorFactoryBean();
		mvcValidator.afterPropertiesSet();
		diaryController = new DiaryController(
				createDiaryUseCase,
				deleteDiaryUseCase,
				getCalendarUseCase,
				updateDiaryUseCase,
				getDiaryGalleryUseCase);
		mockMvc = MockMvcBuilders.standaloneSetup(diaryController)
				.setControllerAdvice(
						new GlobalExceptionHandler(Optional.empty(), problemDetailFactory),
						new DiaryExceptionHandler(problemDetailFactory))
				.setValidator(mvcValidator)
				.setCustomArgumentResolvers(new AuthenticationPrincipalResolver())
				.build();
	}

	@Test
	@DisplayName("POST /api/diary는 필수 diary 파트 누락을 validation Problem Details로 처리한다")
	void createDiaryRejectsMissingDiaryPart() throws Exception {
		mockMvc.perform(multipart("/api/diary"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary"))
				.andExpect(jsonPath("$.fieldErrors.diary").value("필수 요청 파트가 없습니다."));

		then(createDiaryUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /api/diary는 명시적인 빈 사진 파일을 diary Problem Details로 거부한다")
	void createDiaryRejectsExplicitEmptyPhoto() throws Exception {
		willThrow(new DiaryInvalidRequestException("이미지 파일은 비어 있을 수 없습니다."))
				.given(createDiaryUseCase).createDiary(any(), any(), eq("user1"));
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"""
						{"status":"PUBLIC","content":"빈 사진 요청","imageInfos":[{"type":"USER_IMAGE","order":0,"photoIndex":0}],"date":"2026-08-12"}
						""".getBytes());
		MockMultipartFile emptyPhoto = new MockMultipartFile(
				"photos",
				"empty.jpg",
				"image/jpeg",
				new byte[0]);

		mockMvc.perform(multipart("/api/diary")
						.file(diary)
						.file(emptyPhoto))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/invalid-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("이미지 파일은 비어 있을 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("photoLessDiaryRequests")
	@DisplayName("POST /api/diary는 이미지 입력 표현과 관계없이 사진 없는 일기를 생성한다")
	void createDiaryWithoutPhotos(String description, String diaryJson) throws Exception {
		given(createDiaryUseCase.createDiary(any(), any(), eq("user1")))
				.willReturn(new DiaryCreatedResult(10L, "사진 없는 일기"));
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				diaryJson.getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.diaryId").value(10L))
				.andExpect(jsonPath("$.content").value("사진 없는 일기"));

		ArgumentCaptor<CreateDiaryCommand> commandCaptor = ArgumentCaptor.forClass(CreateDiaryCommand.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DiaryPhotoUpload>> uploadsCaptor = ArgumentCaptor.forClass(List.class);
		then(createDiaryUseCase).should().createDiary(commandCaptor.capture(), uploadsCaptor.capture(), eq("user1"));
		assertThat(commandCaptor.getValue().imageInfos()).isNotNull().isEmpty();
		assertThat(uploadsCaptor.getValue()).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("POST /api/diary는 imageInfos의 null 원소를 validation Problem Details로 거부한다")
	void createDiaryRejectsNullImageInformation() throws Exception {
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"""
						{"status":"PUBLIC","content":"사진 없는 일기","imageInfos":[null],"date":"2026-08-12"}
						""".getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
				.andExpect(jsonPath("$.fieldErrors['imageInfos[0]']").exists())
				.andExpect(jsonPath("$.instance").value("/api/diary"));

		then(createDiaryUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidCoreDiaryRequests")
	@DisplayName("POST /api/diary는 필수 일기 값을 validation Problem Details로 검증한다")
	void createDiaryRejectsInvalidCoreFields(String description, String diaryJson, String fieldPath) throws Exception {
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				diaryJson.getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors['" + fieldPath + "']").exists())
				.andExpect(jsonPath("$.instance").value("/api/diary"));

		then(createDiaryUseCase).shouldHaveNoInteractions();
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
	@DisplayName("POST /api/diary는 잘못된 공개범위를 common/malformed-request로 처리한다")
	void createDiaryRejectsInvalidVisibilityAsMalformedRequest() throws Exception {
		MockMultipartFile invalidDiary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"""
						{"status":"UNKNOWN","content":"생성 요청","imageInfos":[],"date":"2026-08-12"}
						""".getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(invalidDiary))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/common/malformed-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("요청 본문을 해석할 수 없습니다."))
				.andExpect(jsonPath("$.instance").value("/api/diary"));

		then(createDiaryUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /api/diary는 사진 읽기 IOException을 전역 예외 처리기로 전달한다")
	void createDiaryPropagatesPhotoReadIOException() throws Exception {
		DiaryDTO diary = mock(DiaryDTO.class);
		given(diary.getStatus()).willReturn(DiaryVisibility.PRIVATE);
		given(diary.getContent()).willReturn("content");
		given(diary.getDate()).willReturn(LocalDate.now());
		given(diary.getImageInfos()).willReturn(List.of());
		MultipartFile unreadablePhoto = mock(MultipartFile.class);
		given(unreadablePhoto.getBytes()).willThrow(new IOException("read failed"));

		assertThatThrownBy(() -> diaryController.createDiary(
				diary,
				List.of(unreadablePhoto),
				new UserPrincipal("user1", "pikume")))
				.isInstanceOf(IOException.class);

		then(createDiaryUseCase).shouldHaveNoInteractions();
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
	@DisplayName("POST /api/diary는 일기 요청 검증 예외를 Diary Problem Details로 처리한다")
	void createDiaryReturnsValidationProblemDetailWhenUseCaseRejectsCommand() throws Exception {
		willThrow(new DiaryInvalidRequestException("미래 날짜에 일기를 작성할 수 없습니다: 2099-01-01"))
				.given(createDiaryUseCase).createDiary(any(), any(), eq("user1"));
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
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/invalid-request"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("미래 날짜에 일기를 작성할 수 없습니다: 2099-01-01"))
				.andExpect(jsonPath("$.instance").value("/api/diary"));
	}

	@Test
	@DisplayName("POST /api/diary는 중복 날짜를 diary/conflict Problem Details로 처리한다")
	void createDiaryReturnsConflictForDuplicateDate() throws Exception {
		LocalDate date = LocalDate.of(2026, 8, 12);
		willThrow(new DuplicateDiaryException(date))
				.given(createDiaryUseCase).createDiary(any(), any(), eq("user1"));
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				"""
						{"status":"PUBLIC","content":"중복 일기","imageInfos":[],"date":"2026-08-12"}
						""".getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/diary/conflict"))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.instance").value("/api/diary"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidImageInformationRequests")
	@DisplayName("POST /api/diary는 잘못된 중첩 이미지 정보를 validation Problem Details로 거부한다")
	void createDiaryRejectsInvalidNestedImageInformation(
			String description,
			String imageInfosJson,
			String fieldPath) throws Exception {
		MockMultipartFile diary = new MockMultipartFile(
				"diary",
				"",
				"application/json",
				("{\"status\":\"PUBLIC\",\"content\":\"생성 요청\",\"imageInfos\":"
						+ imageInfosJson + ",\"date\":\"2026-08-12\"}").getBytes());

		mockMvc.perform(multipart("/api/diary")
						.file(diary))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://api.pikume.com/problems/validation/invalid-request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors['" + fieldPath + "']").exists())
				.andExpect(jsonPath("$.instance").value("/api/diary"));

		then(createDiaryUseCase).shouldHaveNoInteractions();
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
			return parameter.getParameterType().equals(UserPrincipal.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return new UserPrincipal("user1", "pikume");
		}
	}

	private static Stream<Arguments> photoLessDiaryRequests() {
		return Stream.of(
				Arguments.of(
						"imageInfos 생략",
						"""
								{"status":"PUBLIC","content":"사진 없는 일기","date":"2026-08-12"}
								"""),
				Arguments.of(
						"imageInfos null",
						"""
								{"status":"PUBLIC","content":"사진 없는 일기","imageInfos":null,"date":"2026-08-12"}
								"""),
				Arguments.of(
						"imageInfos 빈 목록",
						"""
								{"status":"PUBLIC","content":"사진 없는 일기","imageInfos":[],"date":"2026-08-12"}
								"""));
	}

	private static Stream<Arguments> invalidImageInformationRequests() {
		return Stream.of(
				Arguments.of("이미지 타입 누락", "[{\"order\":0}]", "imageInfos[0].type"),
				Arguments.of("이미지 순서 누락", "[{\"type\":\"USER_IMAGE\",\"photoIndex\":0}]", "imageInfos[0].order"),
				Arguments.of("음수 이미지 순서", "[{\"type\":\"USER_IMAGE\",\"order\":-1,\"photoIndex\":0}]", "imageInfos[0].order"),
				Arguments.of("음수 업로드 인덱스", "[{\"type\":\"USER_IMAGE\",\"order\":0,\"photoIndex\":-1}]", "imageInfos[0].photoIndex"),
				Arguments.of("양수가 아닌 AI 이미지 ID", "[{\"type\":\"AI_IMAGE\",\"order\":0,\"aiPhotoId\":0}]", "imageInfos[0].aiPhotoId"));
	}

	private static Stream<Arguments> invalidCoreDiaryRequests() {
		return Stream.of(
				Arguments.of(
						"공개범위 누락",
						"{\"content\":\"일기\",\"imageInfos\":[],\"date\":\"2026-08-12\"}",
						"status"),
				Arguments.of(
						"본문 공백",
						"{\"status\":\"PUBLIC\",\"content\":\" \",\"imageInfos\":[],\"date\":\"2026-08-12\"}",
						"content"),
				Arguments.of(
						"날짜 누락",
						"{\"status\":\"PUBLIC\",\"content\":\"일기\",\"imageInfos\":[]}",
						"date"));
	}
}
