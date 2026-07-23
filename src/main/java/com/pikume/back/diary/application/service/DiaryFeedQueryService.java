package com.pikume.back.diary.application.service;

import com.pikume.back.diary.application.dto.DiaryFeedCandidateView;
import com.pikume.back.diary.application.dto.DiaryPhotoRow;
import com.pikume.back.diary.application.dto.DiaryPhotoView;
import com.pikume.back.diary.application.dto.DiaryVisibilityScope;
import com.pikume.back.diary.application.dto.VisibleDiaryDetailView;
import com.pikume.back.diary.application.policy.DiaryVisibilityPolicy;
import com.pikume.back.diary.application.port.in.QueryDiaryFeedUseCase;
import com.pikume.back.diary.application.port.out.LoadDiaryFeedPort;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryFeedQueryService implements QueryDiaryFeedUseCase {

	private final LoadDiaryFeedPort loadDiaryPort;
	private final DiaryVisibilityPolicy visibilityPolicy;

	@Override
	public Optional<VisibleDiaryDetailView> findVisibleDiaryDetailById(Long diaryId, String viewerId) {
		return loadDiaryPort.findActiveById(diaryId)
				.filter(diary -> !visibilityPolicy.isHiddenFromViewer(diary, viewerId))
				.map(diary -> new VisibleDiaryDetailView(
						diary.getId(),
						diary.getUserId(),
						toVisibilityScope(diary.getStatus()),
						diary.getContent(),
						loadDiaryPort.findPhotoRowsByDiaryIds(Set.of(diary.getId())).stream()
								.map(this::toPhotoView)
								.sorted(Comparator.comparing(DiaryPhotoView::represent).reversed())
								.toList(),
						diary.getDate(),
						diary.getCreatedAt()));
	}

	@Override
	public List<Long> findDiaryIdsByStatusAndUserIds(
			DiaryVisibilityScope status,
			List<String> userIds,
			int limit) {
		return loadDiaryPort.findRecentIdsByStatusAndUserIds(toDomainVisibility(status), userIds, limit);
	}

	@Override
	public List<Long> findDiaryIdsByStatus(DiaryVisibilityScope status, String excludedUserId, int limit) {
		return loadDiaryPort.findRecentIdsByStatusExcludingUser(
				toDomainVisibility(status),
				excludedUserId,
				limit);
	}

	@Override
	public List<DiaryFeedCandidateView> findLatestVisibleFeedCandidates(
			String viewerId,
			List<String> friendUserIds,
			LocalDate cursorDate,
			Long cursorDiaryId,
			int limit) {
		return loadDiaryPort.findLatestVisibleCandidates(viewerId, friendUserIds, cursorDate, cursorDiaryId, limit);
	}

	private DiaryPhotoView toPhotoView(DiaryPhotoRow row) {
		return new DiaryPhotoView(row.diaryId(), row.displayObjectKey(), row.represent());
	}

	private DiaryVisibility toDomainVisibility(DiaryVisibilityScope visibility) {
		return switch (visibility) {
			case PUBLIC -> DiaryVisibility.PUBLIC;
			case FRIENDS -> DiaryVisibility.FRIENDS;
			case PRIVATE -> DiaryVisibility.PRIVATE;
			case ANONYMOUS -> DiaryVisibility.ANONYMOUS;
		};
	}

	private DiaryVisibilityScope toVisibilityScope(DiaryVisibility visibility) {
		return switch (visibility) {
			case PUBLIC -> DiaryVisibilityScope.PUBLIC;
			case FRIENDS -> DiaryVisibilityScope.FRIENDS;
			case PRIVATE -> DiaryVisibilityScope.PRIVATE;
			case ANONYMOUS -> DiaryVisibilityScope.ANONYMOUS;
		};
	}
}
