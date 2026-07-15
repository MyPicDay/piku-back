package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.application.exception.DiaryNotFoundException;
import com.pikume.back.diary.application.port.out.DeleteDiaryNotificationPort;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryDeletionService")
class DiaryDeletionServiceTest {

	@InjectMocks private DiaryDeletionService service;
	@Mock private LoadDiaryForCommandPort loadDiaryPort;
	@Mock private RecordDiaryPort recordDiaryPort;
	@Mock private DeleteDiaryNotificationPort notificationPort;
	@Mock private TransactionCompletionPort transactionCompletionPort;

	@Test
	@DisplayName("소유자는 일기를 논리 삭제하고 커밋 후 알림을 정리한다")
	void deletesOwnedDiary() {
		Diary diary = new Diary("content", DiaryVisibility.PRIVATE, LocalDate.now(), "user-1");
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));
		willAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).given(transactionCompletionPort).runAfterCommit(any(Runnable.class));

		service.deleteDiary(1L, "user-1");

		assertThat(diary.isDeleted()).isTrue();
		then(recordDiaryPort).should().record(diary);
		then(notificationPort).should().deleteNotificationsByDiaryId(1L);
	}

	@Test
	@DisplayName("없는 일기는 삭제할 수 없다")
	void rejectsMissingDiary() {
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteDiary(1L, "user-1"))
				.isInstanceOf(DiaryNotFoundException.class);
	}

	@Test
	@DisplayName("다른 사용자의 일기는 삭제할 수 없다")
	void rejectsNonOwner() {
		Diary diary = new Diary("content", DiaryVisibility.PRIVATE, LocalDate.now(), "owner");
		given(loadDiaryPort.findActiveById(1L)).willReturn(Optional.of(diary));

		assertThatThrownBy(() -> service.deleteDiary(1L, "viewer"))
				.isInstanceOf(DiaryAccessDeniedException.class);
		then(recordDiaryPort).shouldHaveNoInteractions();
	}
}
