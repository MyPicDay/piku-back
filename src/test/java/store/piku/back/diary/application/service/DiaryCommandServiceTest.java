package store.piku.back.diary.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.diary.adapter.in.web.dto.DiaryDTO;
import store.piku.back.diary.adapter.in.web.dto.DiaryImageInfo;
import store.piku.back.diary.adapter.in.web.dto.ResponseDiaryDTO;
import store.piku.back.diary.application.port.out.*;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.domain.vo.DiaryPhotoType;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.diary.exception.DuplicateDiaryException;
import store.piku.back.file.FileUtil;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import store.piku.back.social.application.port.in.FriendUseCase;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryCommandService")
class DiaryCommandServiceTest {

	@InjectMocks
	private DiaryCommandService diaryCommandService;

	@Mock
	private LoadDiaryPort loadDiaryPort;
	@Mock
	private SaveDiaryPort saveDiaryPort;
	@Mock
	private PhotoStoragePort photoStoragePort;
	@Mock
	private LoadCreativePort loadCreativePort;
	@Mock
	private LoadUserForDiaryPort loadUserForDiaryPort;
	@Mock
	private SendDiaryNotificationPort sendDiaryNotificationPort;
	@Mock
	private FriendUseCase friendUseCase;
	@Mock
	private AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;
	@Mock
	private FileUtil fileUtil;

	private static final String USER_ID = "user-1";
	private RequestMetaInfo requestMetaInfo;

	@BeforeEach
	void setUp() {
		requestMetaInfo = new RequestMetaInfo("https", "localhost", 8080, "localhost:8080",
				"https://localhost:8080/api/diary", "TestAgent", "127.0.0.1");
	}

	@Nested
	@DisplayName("createDiary")
	class CreateDiary {

		@Test
		@DisplayName("사용자 이미지로 공개 일기를 정상 생성한다")
		void createsPublicDiaryWithUserPhotos() throws IOException {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "오늘의 일기",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());
			List<MultipartFile> photos = List.of(photo);

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryDTO.getDate())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));

			ResponseDiaryDTO result = diaryCommandService.createDiary(diaryDTO, photos, USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
			assertThat(result.getContent()).isEqualTo("오늘의 일기");
			then(saveDiaryPort).should().save(any(Diary.class));
			then(photoStoragePort).should().savePhoto(any(Diary.class), eq(photo), eq(USER_ID), eq(0));
		}

		@Test
		@DisplayName("AI 이미지로 일기를 생성한다")
		void createsWithAiImage() throws IOException {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.AI_IMAGE, 0, 100L, null);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "AI 일기",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryDTO.getDate())).willReturn(Optional.empty());
			given(loadCreativePort.existsByIdAndUserId(100L, USER_ID)).willReturn(true);
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));

			DiaryImageGeneration gen = mock(DiaryImageGeneration.class);
			given(gen.getFilePath()).willReturn("ai/image.png");
			given(loadCreativePort.findById(100L)).willReturn(gen);
			given(photoStoragePort.moveToPublic("ai/image.png")).willReturn("public/image.png");

			ResponseDiaryDTO result = diaryCommandService.createDiary(diaryDTO, null, USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
			then(saveDiaryPort).should().savePhoto(any());
			then(loadCreativePort).should().updateDiaryId(eq(100L), any());
		}

		@Test
		@DisplayName("친구 공개 일기 생성 시 알림을 전송한다")
		void notifiesFriendsForFriendsDiary() throws IOException {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.FRIENDS, "친구 일기",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryDTO.getDate())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));
			given(friendUseCase.getFriends(USER_ID)).willReturn(List.of("friend-1", "friend-2"));

			diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo);

			then(sendDiaryNotificationPort).should().notifyFriendsOfNewDiary(
					eq(List.of("friend-1", "friend-2")), eq(USER_ID), any(Diary.class), eq(requestMetaInfo));
		}

		@Test
		@DisplayName("공개 일기에서는 알림을 전송하지 않는다")
		void doesNotNotifyForPublicDiary() throws IOException {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "공개 일기",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryDTO.getDate())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));

			diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo);

			then(sendDiaryNotificationPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("같은 날짜에 일기가 이미 존재하면 예외를 던진다")
		void throwsWhenDuplicateDate() {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			LocalDate date = LocalDate.now();
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "중복 일기",
					new ArrayList<>(List.of(imageInfo)), date);
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());

			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(USER_ID, date))
					.willReturn(Optional.of(new Diary("기존 일기", DiaryVisibility.PUBLIC, date, USER_ID)));

			assertThatThrownBy(
					() -> diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(DuplicateDiaryException.class);
		}

		@Test
		@DisplayName("미래 날짜로 일기 작성 시 예외를 던진다")
		void throwsWhenFutureDate() {
			LocalDate futureDate = LocalDate.now().plusDays(1);
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "미래 일기",
					new ArrayList<>(List.of(imageInfo)), futureDate);
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());

			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(USER_ID, futureDate)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("미래 날짜");
		}

		@Test
		@DisplayName("이미지 순서가 중복되면 예외를 던진다")
		void throwsWhenDuplicateOrder() {
			DiaryImageInfo info1 = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryImageInfo info2 = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 1);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "순서 중복",
					new ArrayList<>(List.of(info1, info2)), LocalDate.now());
			MockMultipartFile photo1 = new MockMultipartFile("p1", "a.jpg", "image/jpeg", "data".getBytes());
			MockMultipartFile photo2 = new MockMultipartFile("p2", "b.jpg", "image/jpeg", "data".getBytes());

			given(fileUtil.getContentType("a.jpg")).willReturn("image/jpeg");
			given(fileUtil.getContentType("b.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(eq(USER_ID), any())).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> diaryCommandService.createDiary(diaryDTO, List.of(photo1, photo2), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("중복");
		}

		@Test
		@DisplayName("허용되지 않는 이미지 확장자이면 예외를 던진다")
		void throwsWhenInvalidImageType() {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "잘못된 확장자",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());
			MockMultipartFile photo = new MockMultipartFile("photo", "test.pdf", "application/pdf", "data".getBytes());

			given(fileUtil.getContentType("test.pdf")).willReturn("application/pdf");

			assertThatThrownBy(
					() -> diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("허용되지 않는");
		}

		@Test
		@DisplayName("메타데이터 분석 실패해도 일기 생성은 성공한다")
		void succeedsEvenIfAnalysisFails() throws IOException {
			DiaryImageInfo imageInfo = new DiaryImageInfo(DiaryPhotoType.USER_IMAGE, 0, null, 0);
			DiaryDTO diaryDTO = new DiaryDTO(DiaryVisibility.PUBLIC, "분석 실패 일기",
					new ArrayList<>(List.of(imageInfo)), LocalDate.now());
			MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "data".getBytes());

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryDTO.getDate())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));
			willThrow(new RuntimeException("분석 오류")).given(analyzeDiaryContentUseCase).analyzeAndSave(any(), any());

			ResponseDiaryDTO result = diaryCommandService.createDiary(diaryDTO, List.of(photo), USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
		}
	}

	@Nested
	@DisplayName("saveAiPhoto")
	class SaveAiPhoto {

		@Test
		@DisplayName("대표 사진(order=0)이면 public으로 이동한다")
		void movesToPublicWhenRepresent() {
			Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			DiaryImageGeneration gen = mock(DiaryImageGeneration.class);
			given(gen.getFilePath()).willReturn("private/ai.png");
			given(loadCreativePort.findById(1L)).willReturn(gen);
			given(photoStoragePort.moveToPublic("private/ai.png")).willReturn("public/ai.png");

			diaryCommandService.saveAiPhoto(diary, 1L, USER_ID, 0);

			then(photoStoragePort).should().moveToPublic("private/ai.png");
			then(gen).should().updateFilePath("public/ai.png");
			then(saveDiaryPort).should().savePhoto(any());
		}

		@Test
		@DisplayName("대표 사진이 아니면 public으로 이동하지 않는다")
		void doesNotMoveWhenNotRepresent() {
			Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			DiaryImageGeneration gen = mock(DiaryImageGeneration.class);
			given(gen.getFilePath()).willReturn("private/ai.png");
			given(loadCreativePort.findById(1L)).willReturn(gen);

			diaryCommandService.saveAiPhoto(diary, 1L, USER_ID, 1);

			then(photoStoragePort).should(never()).moveToPublic(any());
			then(saveDiaryPort).should().savePhoto(any());
		}

		@Test
		@DisplayName("AI 사진 ID가 null이면 아무것도 하지 않는다")
		void doesNothingWhenAiPhotoIdIsNull() {
			Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);

			diaryCommandService.saveAiPhoto(diary, null, USER_ID, 0);

			then(loadCreativePort).shouldHaveNoInteractions();
			then(saveDiaryPort).should(never()).savePhoto(any());
		}
	}
}
