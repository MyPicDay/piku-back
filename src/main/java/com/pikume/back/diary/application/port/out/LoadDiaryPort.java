package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryGalleryRow;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadDiaryPort {

	record DailyCount(LocalDate date, long count) {
	}

	record PhotoRow(Long diaryId, String url, String optimizedUrl, boolean represent) {

		public String displayUrl() {
			if (optimizedUrl != null && !optimizedUrl.isBlank()) {
				return optimizedUrl;
			}
			return url;
		}
	}

	Optional<Diary> findById(Long diaryId);

	List<Diary> findByIds(Collection<Long> diaryIds);

	List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);

	List<Diary> findByUserIdAndStatusesAndDateBetween(String userId, Collection<DiaryVisibility> statuses, LocalDate start, LocalDate end);

	List<DiaryGalleryRow> findGalleryRowsByUserIdAndStatuses(String userId,
			Collection<DiaryVisibility> statuses,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit);

	Optional<Diary> findByUserIdAndDate(String userId, LocalDate date);

	long countByUserId(String userId);

	long countByUserIdAndStatuses(String userId, Collection<DiaryVisibility> statuses);

	List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, Collection<DiaryVisibility> statuses);

	List<DailyCount> countCreatedDiariesByDate(LocalDate startDate, LocalDate endDate);

	long countAllCreatedDiaries();

	long countCreatedDiariesBefore(LocalDateTime cutoffExclusive);

	boolean existsById(Long diaryId);

	Optional<Photo> findRepresentPhotoByDiaryId(Long diaryId);

	List<Photo> findPhotosByDiaryIds(Collection<Long> diaryIds);

	List<PhotoRow> findPhotoRowsByDiaryIds(Collection<Long> diaryIds);

	List<Long> findRecentDiaryIdsByStatusAndUserIds(DiaryVisibility status, Collection<String> userIds, int limit);

	List<Long> findRecentDiaryIdsByStatus(DiaryVisibility status, int limit);

	List<Long> findRecentDiaryIdsByStatusExcludingUser(DiaryVisibility status, String excludedUserId, int limit);

	List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(String excludedUserId, Collection<String> friendUserIds,
			LocalDate cursorDate, Long cursorDiaryId, int limit);
}
