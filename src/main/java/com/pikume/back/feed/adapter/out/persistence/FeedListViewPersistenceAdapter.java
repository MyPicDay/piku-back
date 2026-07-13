package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.application.dto.DiarySummaryView;
import com.pikume.back.diary.application.port.in.QueryDiaryReadUseCase;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.dto.FeedVisibility;
import com.pikume.back.feed.application.port.out.LoadFeedListViewPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedListViewPersistenceAdapter implements LoadFeedListViewPort {

	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final LoadSocialForFeedPort loadSocialForFeedPort;
	private final ResolveImageUrlPort resolveImageUrlPort;

	@Override
	public List<FeedListItemView> loadFeedListItems(List<Long> diaryIds, String currentUserId) {
		if (diaryIds.isEmpty()) {
			return List.of();
		}

		Map<Long, DiarySummaryView> diariesById = queryDiaryReadUseCase.getDiarySummaries(Set.copyOf(diaryIds));
		Map<Long, List<String>> photosByDiaryId = loadPhotoUrls(Set.copyOf(diaryIds));
		Map<String, UserSummaryView> usersById = loadUsers(diariesById.values());
		Map<String, FeedFriendStatus> friendStatusesByUserId = loadSocialForFeedPort.getFriendStatuses(currentUserId, usersById.keySet());
		Map<Long, Long> commentCountsByDiaryId = loadSocialForFeedPort.getCommentCountsForDiaries(diaryIds);
		Map<Long, Long> likeCountsByDiaryId = loadSocialForFeedPort.getLikeCountsForDiaries(diaryIds);
		Set<Long> likedDiaryIds = loadSocialForFeedPort.getLikedDiaryIds(currentUserId, diaryIds);

		return diaryIds.stream()
				.map(diariesById::get)
				.filter(java.util.Objects::nonNull)
				.map(diary -> toFeedListItemView(
						diary,
						photosByDiaryId,
						usersById,
						friendStatusesByUserId,
						commentCountsByDiaryId,
						likeCountsByDiaryId,
						likedDiaryIds))
				.toList();
	}

	private Map<Long, List<String>> loadPhotoUrls(Set<Long> diaryIds) {
		Map<Long, List<String>> photoUrlsByDiaryId = new HashMap<>();

		queryDiaryReadUseCase.getDiaryPhotos(diaryIds).forEach(photo -> photoUrlsByDiaryId
				.computeIfAbsent(photo.diaryId(), ignored -> new java.util.ArrayList<>())
				.add(resolveImageUrlPort.getPhotoUrl(photo.path(), photo.represent())));

		return photoUrlsByDiaryId;
	}

	private Map<String, UserSummaryView> loadUsers(java.util.Collection<DiarySummaryView> diaries) {
		Set<String> userIds = diaries.stream()
				.filter(diary -> diary.status() != DiaryVisibility.ANONYMOUS)
				.map(DiarySummaryView::userId)
				.collect(Collectors.toSet());
		return queryUserSummaryUseCase.queryUserSummaries(userIds);
	}

	private FeedListItemView toFeedListItemView(DiarySummaryView diary,
			Map<Long, List<String>> photosByDiaryId,
			Map<String, UserSummaryView> usersById,
			Map<String, FeedFriendStatus> friendStatusesByUserId,
			Map<Long, Long> commentCountsByDiaryId,
			Map<Long, Long> likeCountsByDiaryId,
			Set<Long> likedDiaryIds) {
		UserSummaryView user = usersById.get(diary.userId());

		return new FeedListItemView(
				diary.diaryId(),
				toFeedVisibility(diary.status()),
				diary.content(),
				photosByDiaryId.getOrDefault(diary.diaryId(), List.of()),
				diary.date(),
				user != null ? user.nickname() : "알 수 없음",
				user != null ? user.avatarPath() : null,
				diary.userId(),
				diary.createdAt(),
				friendStatusesByUserId.getOrDefault(diary.userId(), FeedFriendStatus.NONE),
				commentCountsByDiaryId.getOrDefault(diary.diaryId(), 0L),
				likeCountsByDiaryId.getOrDefault(diary.diaryId(), 0L),
				likedDiaryIds.contains(diary.diaryId()));
	}

	private FeedVisibility toFeedVisibility(DiaryVisibility visibility) {
		return switch (visibility) {
			case PUBLIC -> FeedVisibility.PUBLIC;
			case FRIENDS -> FeedVisibility.FRIENDS;
			case PRIVATE -> FeedVisibility.PRIVATE;
			case ANONYMOUS -> FeedVisibility.ANONYMOUS;
		};
	}
}
