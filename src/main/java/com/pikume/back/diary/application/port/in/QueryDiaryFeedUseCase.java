package com.pikume.back.diary.application.port.in;

import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.VisibleDiaryDetailView;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueryDiaryFeedUseCase {

	Optional<VisibleDiaryDetailView> findVisibleDiaryDetailById(Long diaryId, String viewerId);

	List<Long> findRestorableDiaryIds(List<Long> diaryIds, String viewerId, List<String> friendUserIds);

	List<Long> findDiaryIdsByStatusAndUserIds(DiaryVisibility status, List<String> userIds, int limit);

	List<Long> findDiaryIdsByStatus(DiaryVisibility status, String excludedUserId, int limit);

	List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(String viewerId, List<String> friendUserIds,
			LocalDateTime cursorCreatedAt, Long cursorDiaryId, int limit);
}
