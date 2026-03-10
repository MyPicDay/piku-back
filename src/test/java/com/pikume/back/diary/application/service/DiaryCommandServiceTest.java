package com.pikume.back.diary.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.pikume.back.creative.application.dto.DiaryImageGenerationView;
import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryCreatedResult;
import com.pikume.back.diary.application.dto.DiaryImageCommand;
import com.pikume.back.diary.application.port.out.LoadCreativePort;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.LoadUserForDiaryPort;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.diary.application.port.out.SendDiaryNotificationPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.domain.exception.DiaryNotFoundException;
import com.pikume.back.diary.domain.exception.DuplicateDiaryException;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.util.FileUtil;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.social.application.port.in.FriendUseCase;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

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
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"오늘의 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					LocalDate.now());
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");
			List<UploadedFileData> photos = List.of(photo);

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryCommand.date())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));

			DiaryCreatedResult result = diaryCommandService.createDiary(diaryCommand, photos, USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
			assertThat(result.content()).isEqualTo("오늘의 일기");
			then(saveDiaryPort).should().save(any(Diary.class));
			then(photoStoragePort).should().savePhoto(any(Diary.class), eq(photo), eq(USER_ID), eq(0));
		}

		@Test
		@DisplayName("AI 이미지로 일기를 생성한다")
		void createsWithAiImage() throws IOException {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"AI 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.AI_IMAGE, 0, 100L, null)),
					LocalDate.now());

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryCommand.date())).willReturn(Optional.empty());
			given(loadCreativePort.existsByIdAndUserId(100L, USER_ID)).willReturn(true);
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));
			given(loadCreativePort.findById(100L))
					.willReturn(new DiaryImageGenerationView(100L, USER_ID, "prompt", "ai/image.png", null));
			given(photoStoragePort.moveToPublic("ai/image.png")).willReturn("public/image.png");

			DiaryCreatedResult result = diaryCommandService.createDiary(diaryCommand, null, USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
			then(saveDiaryPort).should().savePhoto(any());
			then(loadCreativePort).should().updateDiaryId(eq(100L), any());
		}

		@Test
		@DisplayName("친구 공개 일기 생성 시 알림을 전송한다")
		void notifiesFriendsForFriendsDiary() throws IOException {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.FRIENDS,
					"친구 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					LocalDate.now());
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryCommand.date())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));
			given(friendUseCase.getFriends(USER_ID)).willReturn(List.of("friend-1", "friend-2"));

			diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo);

			then(sendDiaryNotificationPort).should().notifyFriendsOfNewDiary(
					eq(List.of("friend-1", "friend-2")), eq(USER_ID), any(Diary.class), eq(requestMetaInfo));
		}

		@Test
		@DisplayName("공개 일기에서는 알림을 전송하지 않는다")
		void doesNotNotifyForPublicDiary() throws IOException {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"공개 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					LocalDate.now());
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryCommand.date())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));

			diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo);

			then(sendDiaryNotificationPort).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("같은 날짜에 일기가 이미 존재하면 예외를 던진다")
		void throwsWhenDuplicateDate() {
			LocalDate date = LocalDate.now();
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"중복 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					date);
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");

			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(USER_ID, date))
					.willReturn(Optional.of(new Diary("기존 일기", DiaryVisibility.PUBLIC, date, USER_ID)));

			assertThatThrownBy(() -> diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(DuplicateDiaryException.class);
		}

		@Test
		@DisplayName("미래 날짜로 일기 작성 시 예외를 던진다")
		void throwsWhenFutureDate() {
			LocalDate futureDate = LocalDate.now().plusDays(1);
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"미래 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					futureDate);
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");

			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(USER_ID, futureDate)).willReturn(Optional.empty());

			assertThatThrownBy(() -> diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("미래 날짜");
		}

		@Test
		@DisplayName("이미지 순서가 중복되면 예외를 던진다")
		void throwsWhenDuplicateOrder() {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"순서 중복",
					List.of(
							new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0),
							new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 1)),
					LocalDate.now());
			UploadedFileData photo1 = uploadedFile("a.jpg", "image/jpeg");
			UploadedFileData photo2 = uploadedFile("b.jpg", "image/jpeg");

			given(fileUtil.getContentType("a.jpg")).willReturn("image/jpeg");
			given(fileUtil.getContentType("b.jpg")).willReturn("image/jpeg");
			given(loadDiaryPort.findByUserIdAndDate(eq(USER_ID), any())).willReturn(Optional.empty());

			assertThatThrownBy(() -> diaryCommandService.createDiary(diaryCommand, List.of(photo1, photo2), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("중복");
		}

		@Test
		@DisplayName("허용되지 않는 이미지 확장자이면 예외를 던진다")
		void throwsWhenInvalidImageType() {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"잘못된 확장자",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					LocalDate.now());
			UploadedFileData photo = uploadedFile("test.pdf", "application/pdf");

			given(fileUtil.getContentType("test.pdf")).willReturn("application/pdf");

			assertThatThrownBy(() -> diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("허용되지 않는");
		}

		@Test
		@DisplayName("메타데이터 분석 실패해도 일기 생성은 성공한다")
		void succeedsEvenIfAnalysisFails() throws IOException {
			CreateDiaryCommand diaryCommand = createDiaryCommand(
					DiaryVisibility.PUBLIC,
					"분석 실패 일기",
					List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)),
					LocalDate.now());
			UploadedFileData photo = uploadedFile("test.jpg", "image/jpeg");

			given(loadDiaryPort.findByUserIdAndDate(USER_ID, diaryCommand.date())).willReturn(Optional.empty());
			given(fileUtil.getContentType("test.jpg")).willReturn("image/jpeg");
			given(saveDiaryPort.save(any(Diary.class))).willAnswer(inv -> inv.getArgument(0));
			willThrow(new RuntimeException("분석 오류")).given(analyzeDiaryContentUseCase).analyzeAndSave(anyLong(), anyString());

			DiaryCreatedResult result = diaryCommandService.createDiary(diaryCommand, List.of(photo), USER_ID, requestMetaInfo);

			assertThat(result).isNotNull();
		}
	}

	@Nested
	@DisplayName("deleteDiary")
	class DeleteDiary {

		@Test
		@DisplayName("본인의 일기를 정상 삭제한다")
		void deletesOwnDiary() {
			Diary diary = new Diary("삭제할 일기", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			given(loadDiaryPort.findById(1L)).willReturn(Optional.of(diary));

			diaryCommandService.deleteDiary(1L, USER_ID);

			assertThat(diary.getDeletedAt()).isNotNull();
			then(saveDiaryPort).should().save(diary);
		}

		@Test
		@DisplayName("존재하지 않는 일기 삭제 시 예외를 던진다")
		void throwsWhenDiaryNotFound() {
			given(loadDiaryPort.findById(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> diaryCommandService.deleteDiary(999L, USER_ID))
					.isInstanceOf(DiaryNotFoundException.class);
		}

		@Test
		@DisplayName("타인의 일기 삭제 시 예외를 던진다")
		void throwsWhenNotOwner() {
			Diary diary = new Diary("남의 일기", DiaryVisibility.PUBLIC, LocalDate.now(), "other-user");
			given(loadDiaryPort.findById(1L)).willReturn(Optional.of(diary));

			assertThatThrownBy(() -> diaryCommandService.deleteDiary(1L, USER_ID))
					.isInstanceOf(DiaryAccessDeniedException.class);
		}
	}

	@Nested
	@DisplayName("saveAiPhoto")
	class SaveAiPhoto {

		@Test
		@DisplayName("대표 사진(order=0)이면 public으로 이동한다")
		void movesToPublicWhenRepresent() {
			Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			given(loadCreativePort.findById(1L))
					.willReturn(new DiaryImageGenerationView(1L, USER_ID, "prompt", "private/ai.png", null));
			given(photoStoragePort.moveToPublic("private/ai.png")).willReturn("public/ai.png");

			diaryCommandService.saveAiPhoto(diary, 1L, USER_ID, 0);

			then(photoStoragePort).should().moveToPublic("private/ai.png");
			then(loadCreativePort).should().updateFilePath(1L, "public/ai.png");
			then(saveDiaryPort).should().savePhoto(any());
		}

		@Test
		@DisplayName("대표 사진이 아니면 public으로 이동하지 않는다")
		void doesNotMoveWhenNotRepresent() {
			Diary diary = new Diary("내용", DiaryVisibility.PUBLIC, LocalDate.now(), USER_ID);
			given(loadCreativePort.findById(1L))
					.willReturn(new DiaryImageGenerationView(1L, USER_ID, "prompt", "private/ai.png", null));
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

	private CreateDiaryCommand createDiaryCommand(DiaryVisibility visibility, String content,
			List<DiaryImageCommand> imageInfos, LocalDate date) {
		return new CreateDiaryCommand(visibility, content, new ArrayList<>(imageInfos), date);
	}

	private UploadedFileData uploadedFile(String originalFilename, String contentType) {
		try {
			MockMultipartFile file = new MockMultipartFile("photo", originalFilename, contentType, "data".getBytes());
			return new UploadedFileData(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
