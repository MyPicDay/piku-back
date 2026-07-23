package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryVisibilityScope;
import com.pikume.back.diary.application.dto.VisibleDiaryDetailView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QueryDiaryFeedUseCase {

	Optional<VisibleDiaryDetailView> findVisibleDiaryDetailById(Long diaryId, String viewerId);

	List<Long> findDiaryIdsByStatusAndUserIds(DiaryVisibilityScope status, List<String> userIds, int limit);

	List<Long> findDiaryIdsByStatus(DiaryVisibilityScope status, String excludedUserId, int limit);

	List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(String viewerId, List<String> friendUserIds,
			LocalDate cursorDate, Long cursorDiaryId, int limit);
}
