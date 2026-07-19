package com.pikume.back.feed.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.port.out.LoadFeedCandidateSignalsPort;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadFeedItemEngagementPort;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;
import com.pikume.back.social.application.port.in.CommentUseCase;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.application.port.in.LikeUseCase;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SocialAdapterForFeed implements LoadFeedItemEngagementPort, LoadFeedCandidateSignalsPort,
		LoadFeedFriendshipPort {

	private final FriendUseCase friendUseCase;
	private final LikeUseCase likeUseCase;
	private final CommentUseCase commentUseCase;

	@Override
	public Map<Long, FeedEngagementView> loadEngagements(String currentUserId, List<Long> diaryIds) {
		Map<Long, Long> commentCounts = commentUseCase.getCommentCountsForDiaries(diaryIds);
		Map<Long, Long> likeCounts = likeUseCase.getLikeCountsForDiaries(diaryIds);
		Set<Long> likedDiaryIds = likeUseCase.getLikedDiaryIds(currentUserId, diaryIds);

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
		return friendUseCase.getFriendStatuses(currentUserId, targetUserIds).entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						entry -> toFeedFriendStatus(entry.getValue())));
	}

	@Override
	public Map<Long, Long> loadLikeCounts(List<Long> diaryIds) {
		return likeUseCase.getLikeCountsForDiaries(diaryIds);
	}

	@Override
	public Map<Long, Long> loadCommentCounts(List<Long> diaryIds) {
		return commentUseCase.getCommentCountsForDiaries(diaryIds);
	}

	@Override
	public Set<Long> loadLikedDiaryIds(String currentUserId, List<Long> diaryIds) {
		return likeUseCase.getLikedDiaryIds(currentUserId, diaryIds);
	}

	@Override
	public Set<Long> loadCommentedDiaryIds(String currentUserId, List<Long> diaryIds) {
		return commentUseCase.getCommentedDiaryIds(currentUserId, diaryIds);
	}

	@Override
	public List<String> loadFriendUserIds(String userId) {
		return friendUseCase.getFriends(userId);
	}

	private FeedFriendStatus toFeedFriendStatus(FriendStatus friendStatus) {
		return switch (friendStatus) {
			case NONE -> FeedFriendStatus.NONE;
			case REQUESTED -> FeedFriendStatus.REQUESTED;
			case RECEIVED -> FeedFriendStatus.RECEIVED;
			case FRIENDS -> FeedFriendStatus.FRIENDS;
		};
	}
}
