package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedListViewPersistenceAdapter implements LoadFeedListViewPort {

	private final QueryDiaryReadUseCase queryDiaryReadUseCase;
	private final QueryUserSummaryUseCase queryUserSummaryUseCase;
	private final LoadSocialForFeedPort loadSocialForFeedPort;
	private final ResolveImageUrlPort resolveImageUrlPort;

	@Override
	public List<FeedListItemView> loadFeedListItems(List<Long> diaryIds, String currentUserId) {
		long totalStartedAt = System.nanoTime();
		if (diaryIds.isEmpty()) {
			log.info("event=feed_materialize_completed outcome=success userId={} diaryCount=0 itemCount=0 totalDurationMs={}",
					currentUserId,
					elapsedMillis(totalStartedAt));
			return List.of();
		}

		long stageStartedAt = System.nanoTime();
		Map<Long, DiarySummaryView> diariesById = queryDiaryReadUseCase.getDiarySummaries(Set.copyOf(diaryIds));
		log.info("event=feed_materialize_stage outcome=success stage=load_diary_summaries userId={} diaryCount={} summaryCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				diariesById.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Map<Long, List<String>> photosByDiaryId = loadPhotoUrls(Set.copyOf(diaryIds));
		log.info("event=feed_materialize_stage outcome=success stage=load_photo_urls userId={} diaryCount={} photoOwnerCount={} photoCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				photosByDiaryId.size(),
				photosByDiaryId.values().stream().mapToInt(List::size).sum(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Map<String, UserSummaryView> usersById = loadUsers(diariesById.values());
		log.info("event=feed_materialize_stage outcome=success stage=load_users userId={} authorCount={} loadedUserCount={} durationMs={}",
				currentUserId,
				diariesById.values().stream().map(DiarySummaryView::userId).collect(Collectors.toSet()).size(),
				usersById.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Map<String, FeedFriendStatus> friendStatusesByUserId = loadSocialForFeedPort.getFriendStatuses(currentUserId, usersById.keySet());
		log.info("event=feed_materialize_stage outcome=success stage=load_friend_statuses userId={} targetUserCount={} statusCount={} durationMs={}",
				currentUserId,
				usersById.size(),
				friendStatusesByUserId.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Map<Long, Long> commentCountsByDiaryId = loadSocialForFeedPort.getCommentCountsForDiaries(diaryIds);
		log.info("event=feed_materialize_stage outcome=success stage=load_comment_counts userId={} diaryCount={} countedDiaryCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				commentCountsByDiaryId.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Map<Long, Long> likeCountsByDiaryId = loadSocialForFeedPort.getLikeCountsForDiaries(diaryIds);
		log.info("event=feed_materialize_stage outcome=success stage=load_like_counts userId={} diaryCount={} countedDiaryCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				likeCountsByDiaryId.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		Set<Long> likedDiaryIds = loadSocialForFeedPort.getLikedDiaryIds(currentUserId, diaryIds);
		log.info("event=feed_materialize_stage outcome=success stage=load_liked_statuses userId={} diaryCount={} likedDiaryCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				likedDiaryIds.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		List<FeedListItemView> items = diaryIds.stream()
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
		log.info("event=feed_materialize_stage outcome=success stage=build_items userId={} diaryCount={} itemCount={} durationMs={}",
				currentUserId,
				diaryIds.size(),
				items.size(),
				elapsedMillis(stageStartedAt));
		log.info("event=feed_materialize_completed outcome=success userId={} diaryCount={} itemCount={} totalDurationMs={}",
				currentUserId,
				diaryIds.size(),
				items.size(),
				elapsedMillis(totalStartedAt));

		return items;
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
				.map(DiarySummaryView::userId)
				.collect(Collectors.toSet());
		return queryUserSummaryUseCase.getUserSummaries(userIds);
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
		};
	}

	private long elapsedMillis(long startedAtNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
	}
}
