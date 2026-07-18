package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.UpdateDiaryCommand;
import com.pikume.back.diary.application.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.exception.DiaryNotFoundException;
import com.pikume.back.diary.application.port.out.AnalyzeDiaryContentPort;
import com.pikume.back.diary.application.port.out.LoadDiaryForCommandPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPort;
import com.pikume.back.diary.application.port.out.TransactionCompletionPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryUpdateService")
class DiaryUpdateServiceTest {

	@InjectMocks private DiaryUpdateService service;
	@Mock private LoadDiaryForCommandPort loadDiaryPort;
	@Mock private RecordDiaryPort recordDiaryPort;
	@Mock private DiaryPhotoRelocationService relocationService;
	@Mock private AnalyzeDiaryContentPort analysisPort;
	@Mock private TransactionCompletionPort transactionCompletionPort;

	@Test
	@DisplayName("소유자의 일기 내용과 공개범위를 변경한다")
	void updatesOwnedDiary() {
		Diary diary = diary("user-1");
		var relocation = new DiaryPhotoRelocationService.Relocation(List.of(), List.of());
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(relocationService.relocate(diary, DiaryVisibility.PUBLIC)).willReturn(relocation);
		given(recordDiaryPort.record(diary)).willReturn(diary);
		willAnswer(invocation -> {
			try {
				((Runnable) invocation.getArgument(0)).run();
			} catch (RuntimeException exception) {
				Consumer<RuntimeException> failureHandler = invocation.getArgument(1);
				failureHandler.accept(exception);
			}
			return null;
		}).given(transactionCompletionPort).runAfterCommit(any(Runnable.class), any());

		var result = service.updateDiary(1L, new UpdateDiaryCommand(DiaryVisibility.PUBLIC, "changed"), "user-1");

		assertThat(result.content()).isEqualTo("changed");
		assertThat(diary.getStatus()).isEqualTo(DiaryVisibility.PUBLIC);
		then(relocationService).should().completeAfterTransaction(relocation);
		then(analysisPort).should().analyze(1L, "changed");
	}

	@Test
	@DisplayName("일기 저장 실패 시 새로 복사한 object를 정리한다")
	void cleansRelocationWhenDiaryRecordingFails() {
		Diary diary = diary("user-1");
		var relocation = new DiaryPhotoRelocationService.Relocation(List.of("public/photo.jpg"), List.of("private/photo.jpg"));
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		given(relocationService.relocate(diary, DiaryVisibility.PUBLIC)).willReturn(relocation);
		willThrow(new RuntimeException("db failed")).given(recordDiaryPort).record(diary);

		assertThatThrownBy(() -> service.updateDiary(
				1L, new UpdateDiaryCommand(DiaryVisibility.PUBLIC, "changed"), "user-1"))
				.isInstanceOf(RuntimeException.class);
		then(relocationService).should().rollback(relocation);
	}

	@Test
	@DisplayName("없는 일기는 수정할 수 없다")
	void rejectsMissingDiary() {
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateDiary(
				1L, new UpdateDiaryCommand(DiaryVisibility.PUBLIC, "changed"), "user-1"))
				.isInstanceOf(DiaryNotFoundException.class);
	}

	@Test
	@DisplayName("다른 사용자의 일기는 수정할 수 없다")
	void rejectsNonOwner() {
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary("owner")));

		assertThatThrownBy(() -> service.updateDiary(
				1L, new UpdateDiaryCommand(DiaryVisibility.PUBLIC, "changed"), "viewer"))
				.isInstanceOf(DiaryAccessDeniedException.class);
		then(recordDiaryPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("필수 수정 값이 없으면 일기나 사진을 조회하지 않는다")
	void rejectsInvalidCommandBeforeLoadingDiary() {
		assertThatThrownBy(() -> service.updateDiary(
				1L, new UpdateDiaryCommand(null, "changed"), "user-1"))
				.isInstanceOf(DiaryInvalidRequestException.class)
				.hasMessageContaining("공개범위");

		then(loadDiaryPort).shouldHaveNoInteractions();
		then(relocationService).shouldHaveNoInteractions();
	}

	private Diary diary(String ownerId) {
		Diary diary = new Diary("content", DiaryVisibility.PRIVATE, LocalDate.now(), ownerId);
		ReflectionTestUtils.setField(diary, "id", 1L);
		return diary;
	}
}
