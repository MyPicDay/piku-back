package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.port.out.LoadDiaryForCommandPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.RelocateDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.TransactionCompletionPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryPhotoType;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryPhotoRelocationService")
class DiaryPhotoRelocationServiceTest {

	@Mock private LoadDiaryForCommandPort loadDiaryPort;
	@Mock private RecordDiaryPhotoPort recordDiaryPhotoPort;
	@Mock private RelocateDiaryPhotoPort relocateDiaryPhotoPort;
	@Mock private TransactionCompletionPort transactionCompletionPort;
	private DiaryPhotoRelocationService service;

	@BeforeEach
	void setUp() {
		service = new DiaryPhotoRelocationService(
				loadDiaryPort,
				recordDiaryPhotoPort,
				relocateDiaryPhotoPort,
				transactionCompletionPort);
	}

	@Test
	@DisplayName("공개 저장 영역이 바뀌면 원본과 최적화 object를 복사하고 Photo 참조를 변경한다")
	void relocatesOriginalAndOptimizedObjects() {
		Diary diary = diary(DiaryVisibility.PRIVATE);
		Photo photo = new Photo(diary, "private/photo.jpg", 0, DiaryPhotoType.USER_IMAGE);
		photo.markOptimizationSucceeded("private/photo.webp");
		given(loadDiaryPort.findPhotosByDiaryId(1L)).willReturn(List.of(photo));
		given(relocateDiaryPhotoPort.copyToVisibilityScope(
				"private/photo.jpg", DiaryVisibility.PUBLIC, DiaryPhotoType.USER_IMAGE))
				.willReturn("public/photo.jpg");
		given(relocateDiaryPhotoPort.copyToVisibilityScope(
				"private/photo.webp", DiaryVisibility.PUBLIC, DiaryPhotoType.USER_IMAGE))
				.willReturn("public/photo.webp");

		var relocation = service.relocate(diary, DiaryVisibility.PUBLIC);

		assertThat(photo.getUrl()).isEqualTo("public/photo.jpg");
		assertThat(photo.getOptimizedUrl()).isEqualTo("public/photo.webp");
		assertThat(relocation.copiedObjectKeys()).containsExactly("public/photo.jpg", "public/photo.webp");
		then(recordDiaryPhotoPort).should().record(photo);
	}

	@Test
	@DisplayName("같은 저장 영역의 공개범위 변경은 object를 이동하지 않는다")
	void skipsRelocationWithinSameStorageScope() {
		Diary diary = diary(DiaryVisibility.PUBLIC);

		assertThat(service.relocate(diary, DiaryVisibility.ANONYMOUS).isEmpty()).isTrue();
		then(loadDiaryPort).shouldHaveNoInteractions();
		then(relocateDiaryPhotoPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("사진 저장 실패 시 이미 복사한 새 object를 정리한다")
	void cleansCopiedObjectsWhenRecordingPhotoFails() {
		Diary diary = diary(DiaryVisibility.PRIVATE);
		Photo photo = new Photo(diary, "private/photo.jpg", 0, DiaryPhotoType.USER_IMAGE);
		given(loadDiaryPort.findPhotosByDiaryId(1L)).willReturn(List.of(photo));
		given(relocateDiaryPhotoPort.copyToVisibilityScope(any(), any(), any()))
				.willReturn("public/photo.jpg");
		willThrow(new RuntimeException("db failed")).given(recordDiaryPhotoPort).record(photo);

		assertThatThrownBy(() -> service.relocate(diary, DiaryVisibility.PUBLIC))
				.isInstanceOf(RuntimeException.class);
		then(relocateDiaryPhotoPort).should().delete("public/photo.jpg");
	}

	@Test
	@DisplayName("모든 object 복사가 끝나기 전에는 Photo 참조를 변경하지 않는다")
	void doesNotRecordPartialRelocationWhenLaterCopyFails() {
		Diary diary = diary(DiaryVisibility.PRIVATE);
		Photo firstPhoto = new Photo(diary, "private/first.jpg", 0, DiaryPhotoType.USER_IMAGE);
		Photo secondPhoto = new Photo(diary, "private/second.jpg", 1, DiaryPhotoType.USER_IMAGE);
		given(loadDiaryPort.findPhotosByDiaryId(1L)).willReturn(List.of(firstPhoto, secondPhoto));
		given(relocateDiaryPhotoPort.copyToVisibilityScope(
				"private/first.jpg", DiaryVisibility.PUBLIC, DiaryPhotoType.USER_IMAGE))
				.willReturn("public/first.jpg");
		given(relocateDiaryPhotoPort.copyToVisibilityScope(
				"private/second.jpg", DiaryVisibility.PUBLIC, DiaryPhotoType.USER_IMAGE))
				.willThrow(new RuntimeException("copy failed"));

		assertThatThrownBy(() -> service.relocate(diary, DiaryVisibility.PUBLIC))
				.isInstanceOf(RuntimeException.class);

		assertThat(firstPhoto.getUrl()).isEqualTo("private/first.jpg");
		assertThat(secondPhoto.getUrl()).isEqualTo("private/second.jpg");
		then(recordDiaryPhotoPort).shouldHaveNoInteractions();
		then(relocateDiaryPhotoPort).should().delete("public/first.jpg");
	}

	@Test
	@DisplayName("DB commit 후 이전 object를 정리하고 rollback이면 새 object를 정리한다")
	void registersCommitAndRollbackCleanup() {
		Diary diary = diary(DiaryVisibility.PRIVATE);
		Photo photo = new Photo(diary, "private/photo.jpg", 0, DiaryPhotoType.USER_IMAGE);
		given(loadDiaryPort.findPhotosByDiaryId(1L)).willReturn(List.of(photo));
		given(relocateDiaryPhotoPort.copyToVisibilityScope(any(), any(), any()))
				.willReturn("public/photo.jpg");
		willAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			((Runnable) invocation.getArgument(1)).run();
			return null;
		}).given(transactionCompletionPort).runAfterCompletion(any(Runnable.class), any(Runnable.class));

		var relocation = service.relocate(diary, DiaryVisibility.PUBLIC);
		service.completeAfterTransaction(relocation);

		then(relocateDiaryPhotoPort).should().delete("private/photo.jpg");
		then(relocateDiaryPhotoPort).should().delete("public/photo.jpg");
	}

	private Diary diary(DiaryVisibility visibility) {
		Diary diary = new Diary("content", visibility, LocalDate.now(), "user-1");
		ReflectionTestUtils.setField(diary, "id", 1L);
		return diary;
	}
}
