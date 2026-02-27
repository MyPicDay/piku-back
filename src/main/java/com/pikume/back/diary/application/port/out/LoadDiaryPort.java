package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryMonthCountDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoadDiaryPort {

	Optional<Diary> findById(Long diaryId);

	List<Diary> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);

	Optional<Diary> findByUserIdAndDate(String userId, LocalDate date);

	long countByUserId(String userId);

	List<DiaryMonthCountDTO> countDiariesPerMonth(String userId, LocalDate monthsAgo);

	boolean existsById(Long diaryId);

	Optional<Photo> findRepresentPhotoByDiaryId(Long diaryId);

	// Feed 관련 (Phase 9 분리 전까지 유지)
	List<Diary> findUnreadFeedsByVisibilityAndUserIds(DiaryVisibility status, List<String> friendIds,
			List<Long> excludeIds);

	List<Diary> findUnreadPublicFeeds(List<Long> clickedFeedIds);

	List<Diary> findClickedFeedsAfter(List<Long> clickedFeedIds, LocalDateTime threeDaysAgo);

	List<Diary> findByStatusOrderByCreatedAtDesc(DiaryVisibility status);

	List<Diary> findByStatusAndUserIdIn(DiaryVisibility status, List<String> userIds);
}
