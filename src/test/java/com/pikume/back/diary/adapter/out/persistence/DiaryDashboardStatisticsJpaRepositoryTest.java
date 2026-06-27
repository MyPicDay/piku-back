package com.pikume.back.diary.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DiaryJpaRepository dashboard statistics")
class DiaryDashboardStatisticsJpaRepositoryTest {

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("누적 일기 수는 삭제 여부와 관계없이 현재와 기준 시점까지의 작성 이력을 집계한다")
	void countsAllCreatedDiariesIncludingDeletedRows() {
		LocalDateTime cutoff = LocalDate.of(2026, 6, 16).atStartOfDay();
		insertDiary(1L, cutoff.minusDays(2), null);
		insertDiary(2L, cutoff.minusDays(1), cutoff.plusDays(1));
		insertDiary(3L, cutoff.plusHours(1), null);

		assertThat(diaryJpaRepository.count()).isEqualTo(3);
		assertThat(diaryJpaRepository.countByCreatedAtBefore(cutoff)).isEqualTo(2);
	}

	@Test
	@DisplayName("일간 작성 수는 삭제된 일기도 작성 실적에 포함한다")
	void dailyCountsIncludeDeletedRows() {
		LocalDate date = LocalDate.of(2026, 6, 16);
		insertDiary(1L, date.atTime(10, 0), null);
		insertDiary(2L, date.atTime(11, 0), date.plusDays(1).atStartOfDay());

		var rows = diaryJpaRepository.countCreatedDiariesByDate(
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());

		assertThat(rows).singleElement().satisfies(row ->
				assertThat(row.getMetricCount()).isEqualTo(2L));
	}

	private void insertDiary(Long id, LocalDateTime createdAt, LocalDateTime deletedAt) {
		jdbcTemplate.update("""
				INSERT INTO diary (id, content, status, date, user_id, created_at, updated_at, deleted_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				id,
				"content-" + id,
				"PRIVATE",
				Date.valueOf(createdAt.toLocalDate()),
				"user-1",
				Timestamp.valueOf(createdAt),
				Timestamp.valueOf(createdAt),
				deletedAt == null ? null : Timestamp.valueOf(deletedAt));
	}
}
