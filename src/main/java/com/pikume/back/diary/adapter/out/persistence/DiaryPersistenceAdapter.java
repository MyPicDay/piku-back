package com.pikume.back.diary.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.LoadPhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.diary.application.port.out.SavePhotoOptimizationPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.PhotoOptimizationStatus;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryPersistenceAdapter implements LoadDiaryPort, SaveDiaryPort, LoadPhotoOptimizationPort,
		SavePhotoOptimizationPort {

	private final DiaryJpaRepository diaryJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;

	@Override
	public Optional<Diary> findById(Long diaryId) {
		return diaryJpaRepository.findByIdAndDeletedAtIsNull(diaryId);
	}

	@Override
	public List<Diary> findByIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return diaryJpaRepository.findByIdInAndDeletedAtIsNull(List.copyOf(diaryIds));
	}

	@Override
	public List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end) {
		return diaryJpaRepository.findByUserIdAndDeletedAtIsNullAndDateBetween(userId, start, end);
	}

	@Override
	public List<Diary> findByUserIdAndStatusesAndDateBetween(String userId, Collection<DiaryVisibility> statuses, LocalDate start,
			LocalDate end) {
		return diaryJpaRepository.findByUserIdAndStatusInAndDeletedAtIsNullAndDateBetween(userId, statuses, start, end);
	}

	@Override
	public List<DiaryGalleryRow> findGalleryRowsByUserIdAndStatuses(String userId,
			Collection<DiaryVisibility> statuses,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit) {
		if (statuses == null || statuses.isEmpty() || limit <= 0) {
			return List.of();
		}
		return diaryJpaRepository.findGalleryRowsByUserIdAndStatuses(
				userId,
				statuses,
				cursorDate,
				cursorDiaryId,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public Optional<Diary> findByUserIdAndDate(String userId, LocalDate date) {
		return diaryJpaRepository.findByUserIdAndDateAndDeletedAtIsNull(userId, date);
	}

	@Override
	public long countByUserId(String userId) {
		return diaryJpaRepository.countByUserIdAndDeletedAtIsNull(userId);
	}

	@Override
	public long countByUserIdAndStatuses(String userId, Collection<DiaryVisibility> statuses) {
		return diaryJpaRepository.countByUserIdAndStatusInAndDeletedAtIsNull(userId, statuses);
	}

	@Override
	public List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, Collection<DiaryVisibility> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return List.of();
		}
		return diaryJpaRepository.countDiariesPerMonthByStatuses(userId, statuses);
	}

	@Override
	public List<LoadDiaryPort.DailyCount> countCreatedDiariesByDate(LocalDate startDate, LocalDate endDate) {
		return diaryJpaRepository.countCreatedDiariesByDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadDiaryPort.DailyCount(toLocalDate(row.getMetricDate()), row.getMetricCount()))
				.toList();
	}

	@Override
	public long countAllCreatedDiaries() {
		return diaryJpaRepository.count();
	}

	@Override
	public long countCreatedDiariesBefore(LocalDateTime cutoffExclusive) {
		return diaryJpaRepository.countByCreatedAtBefore(cutoffExclusive);
	}

	@Override
	public boolean existsById(Long diaryId) {
		return diaryJpaRepository.existsByIdAndDeletedAtIsNull(diaryId);
	}

	@Override
	public Optional<Photo> findRepresentPhotoByDiaryId(Long diaryId) {
		return photoJpaRepository.findFirstByDiaryIdAndRepresentIsTrue(diaryId);
	}

	@Override
	public List<Photo> findPhotosByDiaryIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return photoJpaRepository.findByDiaryIds(diaryIds);
	}

	@Override
	public List<LoadDiaryPort.PhotoRow> findPhotoRowsByDiaryIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return photoJpaRepository.findPhotoRowsByDiaryIds(diaryIds).stream()
				.map(row -> new LoadDiaryPort.PhotoRow(
						row.getDiaryId(),
						row.getUrl(),
						row.getOptimizedUrl(),
						Boolean.TRUE.equals(row.getRepresent())))
				.toList();
	}

	@Override
	public List<Long> findRecentDiaryIdsByStatusAndUserIds(DiaryVisibility status, Collection<String> userIds, int limit) {
		if (userIds == null || userIds.isEmpty() || limit <= 0) {
			return List.of();
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdIn(status, userIds,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public List<Long> findRecentDiaryIdsByStatus(DiaryVisibility status, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		return diaryJpaRepository.findFeedIdsByStatus(status, org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public List<Long> findRecentDiaryIdsByStatusExcludingUser(DiaryVisibility status, String excludedUserId, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		if (excludedUserId == null || excludedUserId.isBlank()) {
			return findRecentDiaryIdsByStatus(status, limit);
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdNot(
				status,
				excludedUserId,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(String excludedUserId, Collection<String> friendUserIds,
			LocalDate cursorDate, Long cursorDiaryId, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
		if (friendUserIds == null || friendUserIds.isEmpty()) {
			return diaryJpaRepository.findLatestPublicFeedCandidates(excludedUserId, cursorDate, cursorDiaryId, pageable);
		}
		return diaryJpaRepository.findLatestVisibleFeedCandidatesForFriends(
				excludedUserId,
				friendUserIds,
				cursorDate,
				cursorDiaryId,
				pageable);
	}

	@Override
	public Diary save(Diary diary) {
		return diaryJpaRepository.save(diary);
	}

	@Override
	public Photo savePhoto(Photo photo) {
		return photoJpaRepository.save(photo);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PhotoOptimizationTarget> findPendingPhotoOptimizationTargets(int limit) {
		if (limit <= 0) {
			return List.of();
		}
		return photoJpaRepository.findPendingPhotoOptimizationTargets(
				PhotoOptimizationStatus.PENDING,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	@Transactional
	public boolean claimPhotoOptimization(Integer photoId, LocalDateTime attemptedAt) {
		return photoJpaRepository.claimPhotoOptimization(
				photoId,
				PhotoOptimizationStatus.PENDING,
				PhotoOptimizationStatus.PROCESSING,
				attemptedAt) == 1;
	}

	@Override
	@Transactional
	public void markPhotoOptimizationSucceeded(Integer photoId, String optimizedUrl, LocalDateTime optimizedAt) {
		photoJpaRepository.markPhotoOptimizationSucceeded(
				photoId,
				optimizedUrl,
				optimizedAt,
				PhotoOptimizationStatus.SUCCEEDED);
	}

	@Override
	@Transactional
	public void markPhotoOptimizationFailed(Integer photoId, PhotoOptimizationStatus nextStatus, LocalDateTime attemptedAt) {
		photoJpaRepository.markPhotoOptimizationFailed(photoId, nextStatus, attemptedAt);
	}

	@Override
	@Transactional
	public void markPhotoOptimizationSkipped(Integer photoId, LocalDateTime attemptedAt) {
		photoJpaRepository.markPhotoOptimizationSkipped(photoId, PhotoOptimizationStatus.SKIPPED, attemptedAt);
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
