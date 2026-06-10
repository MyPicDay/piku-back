package com.pikume.back.feed.adapter.out.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryFeedUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadLatestFeedCandidatesPort;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.global.port.out.ResolveImageUrlPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForFeed implements LoadDiaryForFeedPort, LoadLatestFeedCandidatesPort {

	private final QueryDiaryFeedUseCase queryDiaryFeedUseCase;
	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final ResolveImageUrlPort resolveImageUrlPort;

	@Override
	public Optional<FeedDiaryDetailView> findVisibleDiaryById(Long diaryId, String viewerId) {
		return queryDiaryFeedUseCase.findVisibleDiaryDetailById(diaryId, viewerId)
				.map(diary -> new FeedDiaryDetailView(
						diary.diaryId(),
						diary.userId(),
						toFeedVisibility(diary.status()),
						diary.content(),
						diary.photos().stream()
								.map(photo -> resolveImageUrlPort.getPhotoUrl(photo.path(), photo.represent()))
								.toList(),
						diary.date(),
						diary.createdAt()));
	}

	@Override
	public List<Long> findFeedIdsByStatusAndUserIds(FeedVisibility status, List<String> userIds, int limit) {
		return queryDiaryFeedUseCase.findDiaryIdsByStatusAndUserIds(toDiaryVisibility(status), userIds, limit);
	}

	@Override
	public List<Long> findFeedIdsByStatus(FeedVisibility status, String excludedUserId, int limit) {
		return queryDiaryFeedUseCase.findDiaryIdsByStatus(toDiaryVisibility(status), excludedUserId, limit);
	}

	@Override
	public Map<Long, FeedDiaryCandidateView> getFeedDiaryCandidates(Set<Long> diaryIds) {
		return queryDiaryReadUseCase.getDiarySummaries(diaryIds).values().stream()
				.collect(Collectors.toMap(
						DiarySummaryView::diaryId,
						diary -> new FeedDiaryCandidateView(
								diary.diaryId(),
								diary.userId(),
								diary.createdAt())));
	}

	@Override
	public List<FeedLatestCursorCandidate> loadCandidates(String currentUserId, List<String> friendUserIds,
			FeedCursor cursor, int limit) {
		return queryDiaryFeedUseCase.findLatestVisibleFeedCandidates(
						currentUserId,
						friendUserIds,
						cursor != null ? cursor.createdAt() : null,
						cursor != null ? cursor.diaryId() : null,
						limit)
				.stream()
				.map(candidate -> new FeedLatestCursorCandidate(candidate.diaryId(), candidate.createdAt()))
				.toList();
	}

	private FeedVisibility toFeedVisibility(DiaryVisibility visibility) {
		return switch (visibility) {
			case PUBLIC -> FeedVisibility.PUBLIC;
			case FRIENDS -> FeedVisibility.FRIENDS;
			case PRIVATE -> FeedVisibility.PRIVATE;
			case ANONYMOUS -> FeedVisibility.ANONYMOUS;
		};
	}

	private DiaryVisibility toDiaryVisibility(FeedVisibility visibility) {
		return switch (visibility) {
			case PUBLIC -> DiaryVisibility.PUBLIC;
			case FRIENDS -> DiaryVisibility.FRIENDS;
			case PRIVATE -> DiaryVisibility.PRIVATE;
			case ANONYMOUS -> DiaryVisibility.ANONYMOUS;
		};
	}
}
