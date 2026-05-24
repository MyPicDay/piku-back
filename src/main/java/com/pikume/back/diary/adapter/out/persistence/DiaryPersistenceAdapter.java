package com.pikume.back.diary.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.port.out.LoadDiaryPort;
import com.pikume.back.diary.application.port.out.SaveDiaryPort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryPersistenceAdapter implements LoadDiaryPort, SaveDiaryPort {

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
	public List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, LocalDate monthsAgo) {
		return diaryJpaRepository.countDiariesPerMonth(userId, monthsAgo);
	}

	@Override
	public List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, LocalDate monthsAgo, Collection<DiaryVisibility> statuses) {
		return diaryJpaRepository.countDiariesPerMonthByStatuses(userId, monthsAgo, statuses);
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
			LocalDateTime cursorCreatedAt, Long cursorDiaryId, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
		if (friendUserIds == null || friendUserIds.isEmpty()) {
			return diaryJpaRepository.findLatestPublicFeedCandidates(excludedUserId, cursorCreatedAt, cursorDiaryId, pageable);
		}
		return diaryJpaRepository.findLatestVisibleFeedCandidatesForFriends(
				excludedUserId,
				friendUserIds,
				cursorCreatedAt,
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
}
