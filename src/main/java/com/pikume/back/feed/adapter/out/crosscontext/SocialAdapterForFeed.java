package com.pikume.back.feed.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.port.out.LoadFeedCandidateEngagementSignalsPort;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadFeedItemEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;
import com.pikume.back.social.application.dto.FriendshipStatusResult;
import com.pikume.back.social.application.port.in.QueryCommentEngagementUseCase;
import com.pikume.back.social.application.port.in.QueryDiaryLikeEngagementUseCase;
import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SocialAdapterForFeed implements LoadFeedItemEngagementPort, LoadFeedCandidateEngagementSignalsPort,
		LoadFeedFriendshipPort {

	private final QueryFriendshipUseCase queryFriendshipUseCase;
	private final QueryDiaryLikeEngagementUseCase queryDiaryLikeEngagementUseCase;
	private final QueryCommentEngagementUseCase queryCommentEngagementUseCase;

	@Override
	public Map<Long, FeedEngagementView> loadEngagements(String currentUserId, List<Long> diaryIds) {
		Map<Long, Long> commentCounts = queryCommentEngagementUseCase.queryCommentCounts(diaryIds);
		Map<Long, Long> likeCounts = queryDiaryLikeEngagementUseCase.queryLikeCounts(diaryIds);
		Set<Long> likedDiaryIds = queryDiaryLikeEngagementUseCase.queryLikedDiaryIds(currentUserId, diaryIds);

		return diaryIds.stream()
				.distinct()
				.collect(java.util.stream.Collectors.toMap(
						diaryId -> diaryId,
						diaryId -> new FeedEngagementView(
								diaryId,
								commentCounts.getOrDefault(diaryId, 0L),
								likeCounts.getOrDefault(diaryId, 0L),
								likedDiaryIds.contains(diaryId))));
	}

	@Override
	public Map<String, FeedFriendStatus> loadFriendStatuses(String currentUserId, Set<String> targetUserIds) {
		return queryFriendshipUseCase.queryFriendshipStatuses(currentUserId, targetUserIds).entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						entry -> toFeedFriendStatus(entry.getValue())));
	}

	@Override
	public Map<Long, Long> loadLikeCounts(List<Long> diaryIds) {
		return queryDiaryLikeEngagementUseCase.queryLikeCounts(diaryIds);
	}

	@Override
	public Map<Long, Long> loadCommentCounts(List<Long> diaryIds) {
		return queryCommentEngagementUseCase.queryCommentCounts(diaryIds);
	}

	@Override
	public Set<Long> loadLikedDiaryIds(String currentUserId, List<Long> diaryIds) {
		return queryDiaryLikeEngagementUseCase.queryLikedDiaryIds(currentUserId, diaryIds);
	}

	@Override
	public Set<Long> loadCommentedDiaryIds(String currentUserId, List<Long> diaryIds) {
		return queryCommentEngagementUseCase.queryCommentedDiaryIds(currentUserId, diaryIds);
	}

	@Override
	public List<String> loadFriendUserIds(String userId) {
		return queryFriendshipUseCase.queryFriendIds(userId);
	}

	private FeedFriendStatus toFeedFriendStatus(FriendshipStatusResult friendStatus) {
		return switch (friendStatus) {
			case NONE -> FeedFriendStatus.NONE;
			case REQUESTED -> FeedFriendStatus.REQUESTED;
			case RECEIVED -> FeedFriendStatus.RECEIVED;
			case FRIENDS -> FeedFriendStatus.FRIENDS;
		};
	}
}
