package com.pikume.back.diary.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.dto.PhotoOptimizationTarget;
import com.pikume.back.diary.application.port.out.LoadDiaryForCommandPort;
import com.pikume.back.diary.application.port.out.LoadDiaryCalendarPort;
import com.pikume.back.diary.application.port.out.LoadDiaryDetailPort;
import com.pikume.back.diary.application.port.out.LoadDiaryFeedPort;
import com.pikume.back.diary.application.port.out.LoadDiaryGalleryPort;
import com.pikume.back.diary.application.port.out.LoadDiaryReadPort;
import com.pikume.back.diary.application.port.out.LoadDiaryStatisticsPort;
import com.pikume.back.diary.application.port.out.LoadPhotoOptimizationPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPhotoPort;
import com.pikume.back.diary.application.port.out.RecordDiaryPort;
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
public class DiaryPersistenceAdapter implements LoadDiaryForCommandPort,
		LoadDiaryDetailPort, LoadDiaryReadPort, LoadDiaryCalendarPort, LoadDiaryGalleryPort, LoadDiaryFeedPort,
		LoadDiaryStatisticsPort, RecordDiaryPort, RecordDiaryPhotoPort, LoadPhotoOptimizationPort,
		SavePhotoOptimizationPort {

	private final DiaryJpaRepository diaryJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;

	@Override
	public Optional<Diary> findActiveById(Long diaryId) {
		return diaryJpaRepository.findByIdAndDeletedAtIsNull(diaryId);
	}

	@Override
	public List<Diary> findActiveByIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return diaryJpaRepository.findByIdInAndDeletedAtIsNull(List.copyOf(diaryIds));
	}

	@Override
	public List<Diary> findByOwnerAndStatusesAndDateBetween(
			String ownerId,
			Collection<DiaryVisibility> statuses,
			LocalDate start,
			LocalDate end) {
		return diaryJpaRepository.findByUserIdAndStatusInAndDeletedAtIsNullAndDateBetween(ownerId, statuses, start, end);
	}

	@Override
	public List<DiaryGalleryRow> findGalleryRows(
			String ownerId,
			Collection<DiaryVisibility> statuses,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit) {
		if (statuses == null || statuses.isEmpty() || limit <= 0) {
			return List.of();
		}
		return diaryJpaRepository.findGalleryRowsByUserIdAndStatuses(
				ownerId,
				statuses,
				cursorDate,
				cursorDiaryId,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public Optional<Diary> findActiveByUserIdAndDate(String userId, LocalDate date) {
		return diaryJpaRepository.findByUserIdAndDateAndDeletedAtIsNull(userId, date);
	}

	@Override
	public long countByOwnerAndStatuses(String ownerId, Collection<DiaryVisibility> statuses) {
		return diaryJpaRepository.countByUserIdAndStatusInAndDeletedAtIsNull(ownerId, statuses);
	}

	@Override
	public List<DiaryMonthCountDTO> countByOwnerAndStatusesPerMonth(
			String ownerId,
			Collection<DiaryVisibility> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return List.of();
		}
		return diaryJpaRepository.countDiariesPerMonthByStatuses(ownerId, statuses);
	}

	@Override
	public List<DiaryPhotoRow> findRepresentativePhotosByDiaryIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return photoJpaRepository.findRepresentPhotoUrlsByDiaryIds(diaryIds).stream()
				.map(row -> new DiaryPhotoRow(
						row.getDiaryId(),
						row.getUrl(),
						row.getOptimizedUrl(),
						true))
				.toList();
	}

	@Override
	public List<Photo> findPhotosByDiaryId(Long diaryId) {
		if (diaryId == null) {
			return List.of();
		}
		return photoJpaRepository.findByDiaryIds(List.of(diaryId));
	}

	@Override
	public List<DiaryPhotoRow> findPhotoRowsByDiaryIds(Collection<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return List.of();
		}
		return photoJpaRepository.findPhotoRowsByDiaryIds(diaryIds).stream()
				.map(row -> new DiaryPhotoRow(
						row.getDiaryId(),
						row.getUrl(),
						row.getOptimizedUrl(),
						Boolean.TRUE.equals(row.getRepresent())))
				.toList();
	}

	@Override
	public List<Long> findRecentIdsByStatusAndUserIds(DiaryVisibility status, Collection<String> userIds, int limit) {
		if (userIds == null || userIds.isEmpty() || limit <= 0) {
			return List.of();
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdIn(status, userIds,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public List<Long> findRecentIdsByStatusExcludingUser(DiaryVisibility status, String excludedUserId, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		if (excludedUserId == null || excludedUserId.isBlank()) {
			return diaryJpaRepository.findFeedIdsByStatus(
					status,
					org.springframework.data.domain.PageRequest.of(0, limit));
		}
		return diaryJpaRepository.findFeedIdsByStatusAndUserIdNot(
				status,
				excludedUserId,
				org.springframework.data.domain.PageRequest.of(0, limit));
	}

	@Override
	public List<DiaryFeedCandidateView> findLatestVisibleCandidates(
			String excludedUserId,
			Collection<String> friendUserIds,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit) {
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
	public long countAllCreated() {
		return diaryJpaRepository.count();
	}

	@Override
	public long countCreatedBefore(LocalDateTime cutoffExclusive) {
		return diaryJpaRepository.countByCreatedAtBefore(cutoffExclusive);
	}

	@Override
	public List<LoadDiaryStatisticsPort.DailyCount> countCreatedByDate(LocalDate startDate, LocalDate endDate) {
		return diaryJpaRepository.countCreatedDiariesByDate(
						startDate.atStartOfDay(),
						endDate.plusDays(1).atStartOfDay())
				.stream()
				.map(row -> new LoadDiaryStatisticsPort.DailyCount(
						toLocalDate(row.getMetricDate()),
						row.getMetricCount()))
				.toList();
	}

	@Override
	public Diary record(Diary diary) {
		return diaryJpaRepository.save(diary);
	}

	@Override
	public Photo record(Photo photo) {
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
