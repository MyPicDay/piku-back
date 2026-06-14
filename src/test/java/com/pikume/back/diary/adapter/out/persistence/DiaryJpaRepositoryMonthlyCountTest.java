package com.pikume.back.diary.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DiaryJpaRepository monthly count query")
class DiaryJpaRepositoryMonthlyCountTest {

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("월별 일기 수는 전체 기간에서 허용된 공개범위와 미삭제 일기만 집계한다")
	void monthlyCountsIncludeFullHistoryAndExcludeHiddenDeletedAndOtherUsers() {
		String userId = "user-1";
		saveDiary(userId, "recent", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 1));
		saveDiary(userId, "old", DiaryVisibility.PUBLIC, LocalDate.of(2024, 1, 31));
		saveDiary(userId, "old same month", DiaryVisibility.PUBLIC, LocalDate.of(2024, 1, 1));
		saveDiary(userId, "hidden", DiaryVisibility.PRIVATE, LocalDate.of(2026, 6, 2));
		saveDeletedDiary(userId, "deleted", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 3));
		saveDiary("user-2", "other user", DiaryVisibility.PUBLIC, LocalDate.of(2026, 6, 4));
		flushAndClear();

		List<DiaryMonthCountDTO> rows = diaryJpaRepository.countDiariesPerMonthByStatuses(
				userId,
				Set.of(DiaryVisibility.PUBLIC));

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).getYear()).isEqualTo(2026);
		assertThat(rows.get(0).getMonth()).isEqualTo(6);
		assertThat(rows.get(0).getCount()).isEqualTo(1L);
		assertThat(rows.get(1).getYear()).isEqualTo(2024);
		assertThat(rows.get(1).getMonth()).isEqualTo(1);
		assertThat(rows.get(1).getCount()).isEqualTo(2L);
	}

	private Diary saveDiary(String userId, String content, DiaryVisibility visibility, LocalDate date) {
		return diaryJpaRepository.save(new Diary(content, visibility, date, userId));
	}

	private void saveDeletedDiary(String userId, String content, DiaryVisibility visibility, LocalDate date) {
		Diary diary = saveDiary(userId, content, visibility, date);
		diary.delete();
		diaryJpaRepository.save(diary);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
