package com.pikume.back.diary.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Photo WebP migration script")
class PhotoWebpMigrationScriptTest {

	private static final Path MIGRATION = Path.of(
			"src/main/resources/db/migration/V6__add_photo_webp_optimization_columns.sql");

	@Test
	@DisplayName("photos WebP 최적화 컬럼과 기존 row 초기화 규칙을 포함한다")
	void containsPhotoWebpOptimizationColumnsAndBackfillRules() throws Exception {
		assertThat(MIGRATION).exists();

		String sql = Files.readString(MIGRATION);

		assertThat(sql).contains("ALTER TABLE photos");
		assertThat(sql).contains("optimized_url");
		assertThat(sql).contains("optimization_status");
		assertThat(sql).contains("optimized_at");
		assertThat(sql).contains("optimization_attempt_count");
		assertThat(sql).contains("optimization_last_attempt_at");
		assertThat(sql).contains("LOWER(SUBSTRING_INDEX(url, '.', -1)) IN ('jpg', 'jpeg', 'png', 'bmp')");
		assertThat(sql).contains("LOWER(SUBSTRING_INDEX(url, '.', -1)) = 'webp'");
		assertThat(sql).contains("CREATE INDEX idx_photos_optimization_status_attempt");
		assertThat(sql).contains("ON photos (optimization_status, optimization_last_attempt_at, id)");
	}
}
