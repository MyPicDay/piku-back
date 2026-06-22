package com.pikume.back.creative.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DiaryImageGenerationJpaRepository dashboard statistics")
class DiaryImageGenerationDashboardStatisticsJpaRepositoryTest {

	@Autowired
	private DiaryImageGenerationJpaRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("누적 성공 수는 삭제 여부와 관계없이 현재와 기준 시점까지의 생성 이력을 집계한다")
	void countsAllSuccessfulGenerationsIncludingDeletedRows() {
		LocalDateTime cutoff = LocalDate.of(2026, 6, 16).atStartOfDay();
		insertGeneration(1L, cutoff.minusDays(2), null);
		insertGeneration(2L, cutoff.minusDays(1), cutoff.plusDays(1));
		insertGeneration(3L, cutoff.plusHours(1), null);

		assertThat(repository.count()).isEqualTo(3);
		assertThat(repository.countByCreatedAtBefore(cutoff)).isEqualTo(2);
	}

	@Test
	@DisplayName("통합 대시보드 일간 성공 수는 삭제된 생성 이력도 포함한다")
	void dashboardDailyCountsIncludeDeletedRows() {
		LocalDate date = LocalDate.of(2026, 6, 16);
		insertGeneration(1L, date.atTime(10, 0), null);
		insertGeneration(2L, date.atTime(11, 0), date.plusDays(1).atStartOfDay());

		var rows = repository.countAllSuccessfulGenerationsByDate(
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());

		assertThat(rows).singleElement().satisfies(row ->
				assertThat(row.getMetricCount()).isEqualTo(2L));
	}

	@Test
	@DisplayName("기존 통계 일간 성공 수는 삭제되지 않은 생성 이력만 포함한다")
	void existingStatisticsDailyCountsExcludeDeletedRows() {
		LocalDate date = LocalDate.of(2026, 6, 16);
		insertGeneration(1L, date.atTime(10, 0), null);
		insertGeneration(2L, date.atTime(11, 0), date.plusDays(1).atStartOfDay());

		var rows = repository.countSuccessfulGenerationsByDate(
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());

		assertThat(rows).singleElement().satisfies(row ->
				assertThat(row.getMetricCount()).isEqualTo(1L));
	}

	private void insertGeneration(Long id, LocalDateTime createdAt, LocalDateTime deletedAt) {
		jdbcTemplate.update("""
				INSERT INTO diary_image_generation
					(id, user_id, prompt, file_path, diary_id, created_at, updated_at, deleted_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				id,
				"user-1",
				"prompt-" + id,
				"/image/" + id,
				null,
				Timestamp.valueOf(createdAt),
				Timestamp.valueOf(createdAt),
				deletedAt == null ? null : Timestamp.valueOf(deletedAt));
	}
}
