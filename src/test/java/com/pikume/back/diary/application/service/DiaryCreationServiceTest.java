package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.CreateDiaryCommand;
import com.pikume.back.diary.application.dto.DiaryImageCommand;
import com.pikume.back.diary.application.dto.DiaryPhotoUpload;
import com.pikume.back.diary.application.exception.DuplicateDiaryException;
import com.pikume.back.diary.application.policy.DiaryImageFilePolicy;
import com.pikume.back.diary.application.port.out.AnalyzeDiaryContentPort;
import com.pikume.back.diary.application.port.out.LoadDiaryForCommandPort;
import com.pikume.back.diary.application.port.out.LoadFriendshipForDiaryPort;
import com.pikume.back.diary.application.port.out.ManageGeneratedImageForDiaryPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPort;
import com.pikume.back.diary.application.port.out.RelocateDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.SendDiaryNotificationPort;
import com.pikume.back.diary.application.port.out.StoreDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.TransactionCompletionPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryCreationService")
class DiaryCreationServiceTest {

	private static final String USER_ID = "user-1";

	@InjectMocks
	private DiaryCreationService service;
	@Mock private LoadDiaryForCommandPort loadDiaryPort;
	@Mock private RecordDiaryPort recordDiaryPort;
	@Mock private RecordDiaryPhotoPort recordDiaryPhotoPort;
	@Mock private StoreDiaryPhotoPort storeDiaryPhotoPort;
	@Mock private RelocateDiaryPhotoPort relocateDiaryPhotoPort;
	@Mock private ManageGeneratedImageForDiaryPort generatedImagePort;
	@Mock private LoadFriendshipForDiaryPort friendshipPort;
	@Mock private SendDiaryNotificationPort notificationPort;
	@Mock private AnalyzeDiaryContentPort analysisPort;
	@Mock private TransactionCompletionPort transactionCompletionPort;
	@Spy private DiaryImageFilePolicy imageFilePolicy = new DiaryImageFilePolicy();

	@Test
	@DisplayName("사용자 사진을 포함한 일기를 저장한다")
	void createsDiaryWithUserPhoto() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC,
				List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)));
		DiaryPhotoUpload upload = new DiaryPhotoUpload("photo.jpg", "image/jpeg", new byte[] { 1 });
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());
		given(recordDiaryPort.record(any(Diary.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(storeDiaryPhotoPort.store(upload, DiaryVisibility.PUBLIC)).willReturn("public/photo.jpg");

		var result = service.createDiary(command, List.of(upload), USER_ID);

		assertThat(result.content()).isEqualTo(command.content());
		then(storeDiaryPhotoPort).should().store(upload, DiaryVisibility.PUBLIC);
		then(recordDiaryPhotoPort).should().record(any(com.pikume.back.diary.domain.Photo.class));
	}

	@Test
	@DisplayName("사진 메타데이터 저장 실패 시 먼저 저장한 object를 정리한다")
	void cleansUploadedObjectWhenPhotoRecordingFails() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC,
				List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0)));
		DiaryPhotoUpload upload = new DiaryPhotoUpload("photo.jpg", "image/jpeg", new byte[] { 1 });
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());
		given(recordDiaryPort.record(any(Diary.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(storeDiaryPhotoPort.store(upload, DiaryVisibility.PUBLIC)).willReturn("public/photo.jpg");
		willThrow(new RuntimeException("db failed"))
				.given(recordDiaryPhotoPort).record(any(com.pikume.back.diary.domain.Photo.class));

		assertThatThrownBy(() -> service.createDiary(command, List.of(upload), USER_ID))
				.isInstanceOf(RuntimeException.class);

		then(relocateDiaryPhotoPort).should().delete("public/photo.jpg");
	}

	@Test
	@DisplayName("공개 AI 사진 연결 rollback 시 새 object만 정리하고 private 원본은 유지한다")
	void keepsGeneratedSourceAndCleansCopiedObjectOnRollback() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC,
				List.of(new DiaryImageCommand(DiaryPhotoType.AI_IMAGE, 0, 10L, null)));
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());
		given(generatedImagePort.isGeneratedImageOwnedByUser(10L, USER_ID)).willReturn(true);
		given(recordDiaryPort.record(any(Diary.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(generatedImagePort.loadGeneratedImagePath(10L)).willReturn("private/ai.png");
		given(relocateDiaryPhotoPort.copyGeneratedImageToPublic("private/ai.png"))
				.willReturn("public/ai.png");
		willAnswer(invocation -> {
			((Runnable) invocation.getArgument(1)).run();
			return null;
		}).given(transactionCompletionPort).runAfterCompletion(any(Runnable.class), any(Runnable.class));

		service.createDiary(command, List.of(), USER_ID);

		then(generatedImagePort).should().updateGeneratedImagePath(10L, "public/ai.png");
		then(relocateDiaryPhotoPort).should().delete("public/ai.png");
		then(relocateDiaryPhotoPort).should(never()).delete("private/ai.png");
	}

	@Test
	@DisplayName("같은 날짜의 활성 일기가 있으면 생성하지 않는다")
	void rejectsDuplicateDate() {
		CreateDiaryCommand command = command(DiaryVisibility.PRIVATE, List.of());
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date()))
				.willReturn(Optional.of(new Diary("existing", DiaryVisibility.PRIVATE, command.date(), USER_ID)));

		assertThatThrownBy(() -> service.createDiary(command, List.of(), USER_ID))
				.isInstanceOf(DuplicateDiaryException.class);
		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("핵심 입력이 없으면 저장소를 조회하기 전에 요청을 거부한다")
	void rejectsMissingCoreFieldsBeforeLoadingPersistence() {
		CreateDiaryCommand command = new CreateDiaryCommand(null, "content", List.of(), LocalDate.now());

		assertThatThrownBy(() -> service.createDiary(command, List.of(), USER_ID))
				.isInstanceOf(com.pikume.back.diary.application.exception.DiaryInvalidRequestException.class)
				.hasMessageContaining("공개범위");
		then(loadDiaryPort).shouldHaveNoInteractions();
		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("같은 사용자 사진 인덱스를 두 번 참조하면 생성하지 않는다")
	void rejectsDuplicateUserPhotoIndexes() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC, List.of(
				new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 0, null, 0),
				new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 1, null, 0)));
		List<DiaryPhotoUpload> uploads = List.of(
				new DiaryPhotoUpload("first.jpg", "image/jpeg", new byte[] { 1 }),
				new DiaryPhotoUpload("second.jpg", "image/jpeg", new byte[] { 2 }));
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.createDiary(command, uploads, USER_ID))
				.isInstanceOf(com.pikume.back.diary.application.exception.DiaryInvalidRequestException.class)
				.hasMessageContaining("인덱스");

		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("음수 이미지 순서는 생성하지 않는다")
	void rejectsNegativeImageOrder() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC,
				List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, -1, null, 0)));
		List<DiaryPhotoUpload> uploads = List.of(
				new DiaryPhotoUpload("photo.jpg", "image/jpeg", new byte[] { 1 }));
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.createDiary(command, uploads, USER_ID))
				.isInstanceOf(com.pikume.back.diary.application.exception.DiaryInvalidRequestException.class)
				.hasMessageContaining("순서");

		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("이미지 순서는 0부터 빠짐없이 이어져야 한다")
	void rejectsNonContiguousImageOrder() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC,
				List.of(new DiaryImageCommand(DiaryPhotoType.USER_IMAGE, 1, null, 0)));
		List<DiaryPhotoUpload> uploads = List.of(
				new DiaryPhotoUpload("photo.jpg", "image/jpeg", new byte[] { 1 }));
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.createDiary(command, uploads, USER_ID))
				.isInstanceOf(com.pikume.back.diary.application.exception.DiaryInvalidRequestException.class)
				.hasMessageContaining("순서");

		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("친구 공개 알림과 본문 분석은 커밋 이후 실행한다")
	void runsNotificationAndAnalysisAfterCommit() {
		CreateDiaryCommand command = command(DiaryVisibility.FRIENDS, List.of());
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());
		given(recordDiaryPort.record(any(Diary.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(friendshipPort.findFriendIds(USER_ID)).willReturn(List.of("friend-1"));
		willAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).given(transactionCompletionPort).runAfterCommit(any(Runnable.class));

		service.createDiary(command, List.of(), USER_ID);

		then(notificationPort).should().notifyFriendsOfNewDiary(List.of("friend-1"), USER_ID, null);
		then(analysisPort).should().analyze(null, command.content());
	}

	@Test
	@DisplayName("본문 분석 실패는 생성 결과를 되돌리지 않는다")
	void analysisFailureDoesNotFailCreation() {
		CreateDiaryCommand command = command(DiaryVisibility.PUBLIC, List.of());
		given(loadDiaryPort.findActiveByUserIdAndDate(USER_ID, command.date())).willReturn(Optional.empty());
		given(recordDiaryPort.record(any(Diary.class))).willAnswer(invocation -> invocation.getArgument(0));
		willAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).given(transactionCompletionPort).runAfterCommit(any(Runnable.class));
		willThrow(new RuntimeException("analysis failed")).given(analysisPort).analyze(null, command.content());

		assertThat(service.createDiary(command, List.of(), USER_ID).content()).isEqualTo(command.content());
	}

	private CreateDiaryCommand command(DiaryVisibility visibility, List<DiaryImageCommand> images) {
		return new CreateDiaryCommand(visibility, "content", images, LocalDate.now());
	}
}
