package com.pikume.back.feed.adapter.out.social;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.social.application.port.in.CommentUseCase;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.application.port.in.LikeUseCase;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SocialAdapterForFeed implements LoadSocialForFeedPort {

	private final FriendUseCase friendUseCase;
	private final LikeUseCase likeUseCase;
	private final CommentUseCase commentUseCase;

	@Override
	public boolean areFriends(String userId1, String userId2) {
		return friendUseCase.areFriends(userId1, userId2);
	}

	@Override
	public List<String> getFriendIds(String userId) {
		return friendUseCase.getFriends(userId);
	}

	@Override
	public long getLikeCount(Long diaryId) {
		return likeUseCase.getLikeCount(diaryId);
	}

	@Override
	public boolean isLikedByUser(String userId, Long diaryId) {
		return likeUseCase.isLikedByUser(userId, diaryId);
	}

	@Override
	public long countComments(String viewerId, Long diaryId) {
		return commentUseCase.countAllCommentsByDiaryId(viewerId, diaryId);
	}

	@Override
	public Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds) {
		return likeUseCase.getLikeCountsForDiaries(diaryIds);
	}

	@Override
	public Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds) {
		return likeUseCase.getLikedDiaryIds(userId, diaryIds);
	}

	@Override
	public Map<Long, Long> getCommentCountsForDiaries(List<Long> diaryIds) {
		return commentUseCase.getCommentCountsForDiaries(diaryIds);
	}

	@Override
	public Set<Long> getCommentedDiaryIds(String userId, List<Long> diaryIds) {
		return commentUseCase.getCommentedDiaryIds(userId, diaryIds);
	}

	@Override
	public Map<String, FeedFriendStatus> getFriendStatuses(String currentUserId, Set<String> targetUserIds) {
		return friendUseCase.getFriendStatuses(currentUserId, targetUserIds).entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						entry -> toFeedFriendStatus(entry.getValue())));
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
