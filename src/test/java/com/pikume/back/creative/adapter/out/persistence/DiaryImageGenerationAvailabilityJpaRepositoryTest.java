package com.pikume.back.creative.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("DiaryImageGenerationJpaRepository diary availability")
class DiaryImageGenerationAvailabilityJpaRepositoryTest {

	@Autowired
	private DiaryImageGenerationJpaRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("본인 소유이며 폐기되지 않고 다른 일기에 연결되지 않은 생성 이미지만 사용할 수 있다")
	void findsOnlyActiveUnassignedGenerationOwnedByUser() {
		insertGeneration(1L, "user-1", null, null);
		insertGeneration(2L, "user-1", 100L, null);
		insertGeneration(3L, "user-1", null, LocalDateTime.now());

		assertThat(repository.existsByIdAndUserIdAndDiaryIdIsNullAndDeletedAtIsNull(1L, "user-1")).isTrue();
		assertThat(repository.existsByIdAndUserIdAndDiaryIdIsNullAndDeletedAtIsNull(2L, "user-1")).isFalse();
		assertThat(repository.existsByIdAndUserIdAndDiaryIdIsNullAndDeletedAtIsNull(3L, "user-1")).isFalse();
		assertThat(repository.existsByIdAndUserIdAndDiaryIdIsNullAndDeletedAtIsNull(1L, "user-2")).isFalse();
	}

	private void insertGeneration(Long id, String userId, Long diaryId, LocalDateTime deletedAt) {
		LocalDateTime createdAt = LocalDateTime.now().minusMinutes(id);
		jdbcTemplate.update("""
				INSERT INTO diary_image_generation
					(id, user_id, prompt, file_path, diary_id, created_at, updated_at, deleted_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""",
				id,
				userId,
				"prompt-" + id,
				"/image/" + id,
				diaryId,
				Timestamp.valueOf(createdAt),
				Timestamp.valueOf(createdAt),
				deletedAt == null ? null : Timestamp.valueOf(deletedAt));
	}
}
