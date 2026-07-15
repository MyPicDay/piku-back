package com.pikume.back.diary.domain;

import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Diary")
class DiaryTest {

	private static final LocalDate DATE = LocalDate.of(2026, 7, 15);

	@Test
	@DisplayName("목적이 드러나는 생성 행위로 일기를 만든다")
	void createsDiaryThroughFactory() {
		Diary diary = Diary.create("content", DiaryVisibility.PRIVATE, DATE, "user-1");

		assertThat(diary.getContent()).isEqualTo("content");
		assertThat(diary.getStatus()).isEqualTo(DiaryVisibility.PRIVATE);
		assertThat(diary.getDate()).isEqualTo(DATE);
		assertThat(diary.getUserId()).isEqualTo("user-1");
	}

	@Test
	@DisplayName("내용이 비어 있으면 일기를 생성할 수 없다")
	void rejectsBlankContent() {
		assertThatThrownBy(() -> new Diary(" ", DiaryVisibility.PRIVATE, DATE, "user-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("내용");
	}

	@Test
	@DisplayName("내용이 500자를 넘으면 일기를 생성할 수 없다")
	void rejectsContentLongerThanFiveHundredCharacters() {
		assertThatThrownBy(() -> new Diary("a".repeat(501), DiaryVisibility.PRIVATE, DATE, "user-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("500");
	}

	@Test
	@DisplayName("필수 값이 없으면 일기를 생성할 수 없다")
	void rejectsMissingRequiredValues() {
		assertThatThrownBy(() -> new Diary("content", null, DATE, "user-1"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Diary("content", DiaryVisibility.PRIVATE, null, "user-1"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Diary("content", DiaryVisibility.PRIVATE, DATE, " "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("삭제된 일기는 다시 수정할 수 없다")
	void deletedDiaryCannotBeUpdated() {
		Diary diary = diary();
		diary.delete();

		assertThatThrownBy(() -> diary.updateContentAndStatus("changed", DiaryVisibility.PUBLIC))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("삭제");
	}

	@Test
	@DisplayName("공개 저장 영역이 달라질 때만 사진 이동이 필요하다")
	void identifiesPhotoScopeTransition() {
		assertThat(DiaryVisibility.PUBLIC.requiresPhotoScopeTransitionTo(DiaryVisibility.ANONYMOUS)).isFalse();
		assertThat(DiaryVisibility.PRIVATE.requiresPhotoScopeTransitionTo(DiaryVisibility.FRIENDS)).isFalse();
		assertThat(DiaryVisibility.PUBLIC.requiresPhotoScopeTransitionTo(DiaryVisibility.PRIVATE)).isTrue();
		assertThat(DiaryVisibility.FRIENDS.requiresPhotoScopeTransitionTo(DiaryVisibility.ANONYMOUS)).isTrue();
	}

	private Diary diary() {
		return new Diary("content", DiaryVisibility.PRIVATE, DATE, "user-1");
	}
}
