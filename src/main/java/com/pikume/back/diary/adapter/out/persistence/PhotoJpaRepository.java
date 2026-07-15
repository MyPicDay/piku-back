package com.pikume.back.diary.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.PhotoOptimizationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PhotoJpaRepository extends JpaRepository<Photo, Integer> {

	interface DiaryThumbnailProjection {
		Long getDiaryId();

		String getUrl();

		String getOptimizedUrl();
	}

	interface DiaryPhotoRowProjection {
		Long getDiaryId();

		String getUrl();

		String getOptimizedUrl();

		Boolean getRepresent();
	}

	@Query("SELECT p FROM Photo p " +
			"WHERE p.diary.id IN :diaryIds " +
			"AND p.diary.deletedAt IS NULL " +
			"ORDER BY p.diary.id ASC, p.represent DESC, p.photoOrder ASC")
	List<Photo> findByDiaryIds(@Param("diaryIds") Collection<Long> diaryIds);

	@Query("SELECT p.diary.id AS diaryId, p.url AS url, p.optimizedUrl AS optimizedUrl " +
			"FROM Photo p " +
			"WHERE p.diary.id IN :diaryIds " +
			"AND p.represent = true " +
			"AND p.diary.deletedAt IS NULL")
	List<DiaryThumbnailProjection> findRepresentPhotoUrlsByDiaryIds(@Param("diaryIds") Collection<Long> diaryIds);

	@Query("SELECT p.diary.id AS diaryId, p.url AS url, p.optimizedUrl AS optimizedUrl, p.represent AS represent " +
			"FROM Photo p " +
			"WHERE p.diary.id IN :diaryIds " +
			"AND p.diary.deletedAt IS NULL " +
			"ORDER BY p.diary.id ASC, p.represent DESC, p.photoOrder ASC")
	List<DiaryPhotoRowProjection> findPhotoRowsByDiaryIds(@Param("diaryIds") Collection<Long> diaryIds);

	@Query("SELECT new com.pikume.back.diary.application.dto.PhotoOptimizationTarget(" +
			"p.id, p.diary.id, p.url, COALESCE(p.optimizationAttemptCount, 0)) " +
			"FROM Photo p " +
			"WHERE p.optimizationStatus = :status " +
			"AND p.optimizedUrl IS NULL " +
			"AND p.diary.deletedAt IS NULL " +
			"ORDER BY p.optimizationLastAttemptAt ASC, p.id ASC")
	List<PhotoOptimizationTarget> findPendingPhotoOptimizationTargets(
			@Param("status") PhotoOptimizationStatus status,
			Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Photo p " +
			"SET p.optimizationStatus = :processingStatus, " +
			"p.optimizationLastAttemptAt = :attemptedAt " +
			"WHERE p.id = :photoId " +
			"AND p.optimizationStatus = :pendingStatus " +
			"AND p.optimizedUrl IS NULL " +
			"AND p.diary.deletedAt IS NULL")
	int claimPhotoOptimization(
			@Param("photoId") Integer photoId,
			@Param("pendingStatus") PhotoOptimizationStatus pendingStatus,
			@Param("processingStatus") PhotoOptimizationStatus processingStatus,
			@Param("attemptedAt") LocalDateTime attemptedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Photo p " +
			"SET p.optimizedUrl = :optimizedUrl, " +
			"p.optimizedAt = :optimizedAt, " +
			"p.optimizationStatus = :status " +
			"WHERE p.id = :photoId")
	int markPhotoOptimizationSucceeded(
			@Param("photoId") Integer photoId,
			@Param("optimizedUrl") String optimizedUrl,
			@Param("optimizedAt") LocalDateTime optimizedAt,
			@Param("status") PhotoOptimizationStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Photo p " +
			"SET p.optimizationAttemptCount = COALESCE(p.optimizationAttemptCount, 0) + 1, " +
			"p.optimizationLastAttemptAt = :attemptedAt, " +
			"p.optimizationStatus = :nextStatus " +
			"WHERE p.id = :photoId")
	int markPhotoOptimizationFailed(
			@Param("photoId") Integer photoId,
			@Param("nextStatus") PhotoOptimizationStatus nextStatus,
			@Param("attemptedAt") LocalDateTime attemptedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Photo p " +
			"SET p.optimizationStatus = :status, " +
			"p.optimizationLastAttemptAt = :attemptedAt " +
			"WHERE p.id = :photoId")
	int markPhotoOptimizationSkipped(
			@Param("photoId") Integer photoId,
			@Param("status") PhotoOptimizationStatus status,
			@Param("attemptedAt") LocalDateTime attemptedAt);
}
