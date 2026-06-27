package com.pikume.back.creative.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pikume.back.creative.domain.DiaryImageGeneration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryImageGenerationJpaRepository extends JpaRepository<DiaryImageGeneration, Long> {

	interface DailyCountProjection {
		Object getMetricDate();

		Long getMetricCount();
	}

	List<DiaryImageGeneration> findByDiaryIdIsNull();

	Optional<DiaryImageGeneration> findByUserIdAndFilePath(String userId, String filePath);

	boolean existsByIdAndUserId(Long id, String userId);

	@Query(value = """
			SELECT CAST(created_at AS DATE) AS metricDate, COUNT(*) AS metricCount
			FROM diary_image_generation
			WHERE created_at >= :startDateTime
			  AND created_at < :endExclusiveDateTime
			  AND deleted_at IS NULL
			GROUP BY CAST(created_at AS DATE)
			""", nativeQuery = true)
	List<DailyCountProjection> countSuccessfulGenerationsByDate(
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime);

	@Query(value = """
			SELECT CAST(created_at AS DATE) AS metricDate, COUNT(*) AS metricCount
			FROM diary_image_generation
			WHERE created_at >= :startDateTime
			  AND created_at < :endExclusiveDateTime
			GROUP BY CAST(created_at AS DATE)
			""", nativeQuery = true)
	List<DailyCountProjection> countAllSuccessfulGenerationsByDate(
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endExclusiveDateTime") LocalDateTime endExclusiveDateTime);

	long countByCreatedAtBefore(LocalDateTime cutoffExclusive);
}
