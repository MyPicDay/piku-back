package com.pikume.back.admin.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("통합 대시보드 인덱스 마이그레이션")
class AdminDashboardIndexMigrationTest {

	@Test
	@DisplayName("누적·기간 집계 원천 테이블에 조회 기준 인덱스를 추가한다")
	void addsDashboardAggregationIndexes() throws IOException {
		ClassPathResource migration = new ClassPathResource(
				"db/migration/V14__add_integrated_dashboard_indexes.sql");
		String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		assertThat(sql).contains(
				"idx_users_created_deleted",
				"idx_users_deleted_created",
				"idx_diary_created",
				"idx_diary_image_generation_created",
				"idx_admin_statistics_events_type_date_user");
	}
}
