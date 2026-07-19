package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.policy.FeedAuthorMaskingPolicy;
import com.pikume.back.feed.application.port.out.LoadFeedAuthorsPort;
import com.pikume.back.feed.application.port.out.LoadFeedDiaryItemsPort;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadFeedItemEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedAuthorView;
import com.pikume.back.feed.application.readmodel.FeedDiaryItemSourceView;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.application.readmodel.FeedPhotoReferenceView;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedListItemAssembler {

	private final LoadFeedDiaryItemsPort loadFeedDiaryItemsPort;
	private final LoadFeedAuthorsPort loadFeedAuthorsPort;
	private final LoadFeedItemEngagementPort loadFeedItemEngagementPort;
	private final LoadFeedFriendshipPort loadFeedFriendshipPort;
	private final FeedAuthorMaskingPolicy feedAuthorMaskingPolicy;

	public List<FeedListItemView> assemble(List<Long> diaryIds, String currentUserId) {
		if (diaryIds.isEmpty()) {
			return List.of();
		}

		Map<Long, FeedDiaryItemSourceView> diariesById = loadFeedDiaryItemsPort.loadDiaryItems(Set.copyOf(diaryIds));
		Set<String> visibleAuthorIds = diariesById.values().stream()
				.filter(diary -> diary.status() != FeedVisibility.ANONYMOUS)
				.map(FeedDiaryItemSourceView::userId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<String, FeedAuthorView> authorsById = loadFeedAuthorsPort.loadAuthors(visibleAuthorIds);
		Map<String, FeedFriendStatus> friendStatusesByUserId =
				loadFeedFriendshipPort.loadFriendStatuses(currentUserId, visibleAuthorIds);
		Map<Long, FeedEngagementView> engagementsByDiaryId =
				loadFeedItemEngagementPort.loadEngagements(currentUserId, diaryIds);

		return diaryIds.stream()
				.map(diariesById::get)
				.filter(Objects::nonNull)
				.map(diary -> assembleItem(
						diary,
						authorsById,
						friendStatusesByUserId,
						engagementsByDiaryId,
						currentUserId))
				.toList();
	}

	private FeedListItemView assembleItem(
			FeedDiaryItemSourceView diary,
			Map<String, FeedAuthorView> authorsById,
			Map<String, FeedFriendStatus> friendStatusesByUserId,
			Map<Long, FeedEngagementView> engagementsByDiaryId,
			String currentUserId
	) {
		FeedAuthorMaskingPolicy.AuthorPresentation author = feedAuthorMaskingPolicy.present(
				diary.status(),
				diary.userId(),
				authorsById.get(diary.userId()),
				friendStatusesByUserId.getOrDefault(diary.userId(), FeedFriendStatus.NONE),
				currentUserId);
		FeedEngagementView engagement = engagementsByDiaryId.getOrDefault(
				diary.diaryId(),
				new FeedEngagementView(diary.diaryId(), 0L, 0L, false));
		List<String> imageUrls = diary.photos() == null
				? List.of()
				: diary.photos().stream()
						.map(FeedPhotoReferenceView::imageUrl)
						.toList();

		return new FeedListItemView(
				diary.diaryId(),
				diary.status(),
				diary.content(),
				imageUrls,
				diary.date(),
				author.nickname(),
				author.avatarUrl(),
				author.userId(),
				diary.createdAt(),
				author.friendStatus(),
				engagement.commentCount(),
				engagement.likeCount(),
				engagement.liked(),
				author.owner());
	}
}
