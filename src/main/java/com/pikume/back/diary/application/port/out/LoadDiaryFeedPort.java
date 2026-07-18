package com.pikume.back.diary.application.port.out;

import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadDiaryFeedPort {

	Optional<Diary> findActiveById(Long diaryId);

	List<DiaryPhotoRow> findPhotoRowsByDiaryIds(Collection<Long> diaryIds);

	List<Long> findRecentIdsByStatusAndUserIds(DiaryVisibility status, Collection<String> userIds, int limit);

	List<Long> findRecentIdsByStatusExcludingUser(DiaryVisibility status, String excludedUserId, int limit);

	List<DiaryFeedCandidateView> findLatestVisibleCandidates(
			String excludedUserId,
			Collection<String> friendUserIds,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit);
}
