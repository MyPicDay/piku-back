package com.pikume.back.feed.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryFeedUseCase;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryCandidateSourcePort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryDetailPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryItemSourcesPort;
import com.pikume.back.feed.application.port.out.LoadLatestFeedCandidatesPort;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;
import com.pikume.back.feed.application.readmodel.FeedPhotoReferenceView;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiaryAdapterForFeed implements LoadFeedDiaryDetailPort, LoadLatestFeedCandidatesPort,
		LoadFeedDiaryItemSourcesPort, LoadFeedDiaryCandidateSourcePort {

	private final QueryDiaryFeedUseCase queryDiaryFeedUseCase;
	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final ResolveObjectUrlPort resolveObjectUrlPort;

	@Override
	public Optional<FeedDiaryDetailView> loadVisibleDiary(Long diaryId, String viewerId) {
		return queryDiaryFeedUseCase.findVisibleDiaryDetailById(diaryId, viewerId)
				.map(diary -> new FeedDiaryDetailView(
						diary.diaryId(),
						diary.userId(),
						toFeedVisibility(diary.status()),
						diary.content(),
						diary.photos().stream()
								.map(photo -> resolveObjectUrlPort.resolveObjectUrl(
										photo.path(),
										isPublicObjectKey(photo.path())))
								.toList(),
						diary.date(),
						diary.createdAt()));
	}

	@Override
	public List<Long> loadRecentDiaryIdsByVisibilityAndAuthors(
			FeedVisibility visibility,
			List<String> authorIds,
			int limit
	) {
		return queryDiaryFeedUseCase.findDiaryIdsByStatusAndUserIds(
				toDiaryVisibility(visibility),
				authorIds,
				limit);
	}

	@Override
	public List<Long> loadRecentDiaryIdsByVisibility(
			FeedVisibility visibility,
			int limit
	) {
		return queryDiaryFeedUseCase.findDiaryIdsByStatus(
				toDiaryVisibility(visibility),
				null,
				limit);
	}

	@Override
	public List<Long> loadRecentDiaryIdsByVisibilityExcludingAuthor(
			FeedVisibility visibility,
			String excludedAuthorId,
			int limit
	) {
		return queryDiaryFeedUseCase.findDiaryIdsByStatus(
				toDiaryVisibility(visibility),
				excludedAuthorId,
				limit);
	}

	@Override
	public Map<Long, FeedDiaryCandidateView> loadCandidateAttributes(Set<Long> diaryIds) {
		return queryDiaryReadUseCase.getDiarySummaries(diaryIds).values().stream()
				.collect(Collectors.toMap(
						DiarySummaryView::diaryId,
						diary -> new FeedDiaryCandidateView(
								diary.diaryId(),
								diary.userId(),
								diary.createdAt())));
	}

	@Override
	public Map<Long, FeedDiaryItemSourceView> loadDiaryItemSources(Set<Long> diaryIds) {
		if (diaryIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<FeedPhotoReferenceView>> photosByDiaryId = new HashMap<>();
		queryDiaryReadUseCase.getDiaryPhotos(diaryIds).forEach(photo -> photosByDiaryId
				.computeIfAbsent(photo.diaryId(), ignored -> new ArrayList<>())
				.add(new FeedPhotoReferenceView(
						resolveObjectUrlPort.resolveObjectUrl(
								photo.path(),
								isPublicObjectKey(photo.path())))));

		return queryDiaryReadUseCase.getDiarySummaries(diaryIds).values().stream()
				.collect(Collectors.toMap(
						DiarySummaryView::diaryId,
						diary -> new FeedDiaryItemSourceView(
								diary.diaryId(),
								diary.userId(),
								toFeedVisibility(diary.status()),
								diary.content(),
								photosByDiaryId.getOrDefault(diary.diaryId(), List.of()),
								diary.date(),
								diary.createdAt())));
	}

	@Override
	public List<FeedLatestCursorCandidate> loadCandidates(String currentUserId, List<String> friendUserIds,
			FeedCursor cursor, int limit) {
		return queryDiaryFeedUseCase.findLatestVisibleFeedCandidates(
						currentUserId,
						friendUserIds,
						cursor != null ? cursor.date() : null,
						cursor != null ? cursor.diaryId() : null,
						limit)
				.stream()
				.map(candidate -> new FeedLatestCursorCandidate(candidate.diaryId(), candidate.date()))
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

	private boolean isPublicObjectKey(String objectKey) {
		return objectKey != null && objectKey.startsWith("public/");
	}
}
