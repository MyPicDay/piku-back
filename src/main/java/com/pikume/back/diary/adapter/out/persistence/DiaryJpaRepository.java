package com.pikume.back.diary.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiaryJpaRepository extends JpaRepository<Diary, Long> {

	Optional<Diary> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	List<Diary> findByIdInAndDeletedAtIsNull(List<Long> ids);

	@Query("SELECT d.id FROM Diary d " +
			"WHERE d.id IN :ids " +
			"AND d.deletedAt IS NULL " +
			"AND (:currentUserId IS NULL OR d.userId <> :currentUserId) " +
			"AND (d.status = com.pikume.back.diary.domain.vo.DiaryVisibility.PUBLIC " +
			"OR (d.status = com.pikume.back.diary.domain.vo.DiaryVisibility.FRIENDS AND d.userId IN :friendIds))")
	List<Long> findRestorableFeedIds(@Param("ids") Collection<Long> ids,
			@Param("currentUserId") String currentUserId,
			@Param("friendIds") Collection<String> friendIds);

	@Query("SELECT d.id FROM Diary d " +
			"WHERE d.id IN :ids " +
			"AND d.deletedAt IS NULL " +
			"AND (:currentUserId IS NULL OR d.userId <> :currentUserId) " +
			"AND d.status = com.pikume.back.diary.domain.vo.DiaryVisibility.PUBLIC")
	List<Long> findRestorablePublicFeedIds(@Param("ids") Collection<Long> ids,
			@Param("currentUserId") String currentUserId);

	List<Diary> findByUserIdAndDeletedAtIsNullAndDateBetween(String userId, LocalDate start, LocalDate end);

	Optional<Diary> findByUserIdAndDateAndDeletedAtIsNull(String userId, LocalDate date);

	long countByUserIdAndDeletedAtIsNull(String userId);

	@Query(value = "SELECT new com.pikume.back.diary.application.dto.DiaryMonthCountDTO(YEAR(d.date), MONTH(d.date), COUNT(d.id)) "
			+
			"FROM Diary d " +
			"WHERE d.userId = :userId " +
			"AND d.deletedAt IS NULL " +
			"AND d.date >= :monthsAgo " +
			"GROUP BY YEAR(d.date), MONTH(d.date) " +
			"ORDER BY YEAR(d.date) DESC, MONTH(d.date) DESC")
	List<DiaryMonthCountDTO> countDiariesPerMonth(
			@Param("userId") String userId,
			@Param("monthsAgo") LocalDate monthsAgo);

	// Feed 관련 쿼리 (Phase 9에서 분리 예정)
	@Query("SELECT d FROM Diary d " +
			"WHERE d.status = :status " +
			"AND d.userId IN :friendIds " +
			"AND d.id NOT IN :excludeIds " +
			"AND d.deletedAt IS NULL " +
			"ORDER BY d.createdAt DESC")
	List<Diary> findUnreadFeedsByVisibilityAndUserIds(
			@Param("status") DiaryVisibility status,
			@Param("friendIds") List<String> friendIds,
			@Param("excludeIds") List<Long> excludeIds);

	@Query("SELECT d FROM Diary d WHERE d.status = 'PUBLIC' AND d.id NOT IN :clickedFeedIds AND d.deletedAt IS NULL ORDER BY d.createdAt DESC")
	List<Diary> findUnreadPublicFeeds(@Param("clickedFeedIds") List<Long> clickedFeedIds);

	@Query("SELECT d FROM Diary d WHERE d.id IN :clickedFeedIds AND d.createdAt > :threeDaysAgo AND d.deletedAt IS NULL")
	List<Diary> findClickedFeedsAfter(@Param("clickedFeedIds") List<Long> clickedFeedIds,
			@Param("threeDaysAgo") LocalDateTime threeDaysAgo);

	List<Diary> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(DiaryVisibility status);

	@Query("SELECT d FROM Diary d " +
			"WHERE d.status = :status " +
			"AND d.userId IN :userIds " +
			"AND d.deletedAt IS NULL " +
			"ORDER BY d.createdAt DESC")
	List<Diary> findByStatusAndUserIdInAndDeletedAtIsNull(@Param("status") DiaryVisibility status, @Param("userIds") List<String> userIds);

	@Query("SELECT d.id FROM Diary d " +
			"WHERE d.status = :status " +
			"AND d.userId IN :userIds " +
			"AND d.deletedAt IS NULL " +
			"ORDER BY d.createdAt DESC")
	List<Long> findFeedIdsByStatusAndUserIdIn(@Param("status") DiaryVisibility status,
			@Param("userIds") Collection<String> userIds,
			Pageable pageable);

	@Query("SELECT d.id FROM Diary d " +
			"WHERE d.status = :status " +
			"AND d.deletedAt IS NULL " +
			"ORDER BY d.createdAt DESC")
	List<Long> findFeedIdsByStatus(@Param("status") DiaryVisibility status, Pageable pageable);

	@Query("SELECT d.id FROM Diary d " +
			"WHERE d.status = :status " +
			"AND d.userId <> :excludedUserId " +
			"AND d.deletedAt IS NULL " +
			"ORDER BY d.createdAt DESC")
	List<Long> findFeedIdsByStatusAndUserIdNot(@Param("status") DiaryVisibility status,
			@Param("excludedUserId") String excludedUserId,
			Pageable pageable);
}
