package store.piku.back.feed.adapter.out.social;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import store.piku.back.feed.application.port.out.LoadSocialForFeedPort;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.social.application.port.in.CommentUseCase;
import store.piku.back.social.application.port.in.FriendUseCase;
import store.piku.back.social.application.port.in.LikeUseCase;
import store.piku.back.social.domain.friend.vo.FriendStatus;

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
	public List<String> getFriendIds(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo) {
		return friendUseCase.findFriendIdList(pageable, userId, requestMetaInfo);
	}

	@Override
	public FriendStatus getFriendshipStatus(String userId, String targetUserId) {
		return friendUseCase.getFriendshipStatus(userId, targetUserId);
	}

	@Override
	public long getLikeCount(Long diaryId) {
		return likeUseCase.getLikeCount(diaryId);
	}

	@Override
	public Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds) {
		return likeUseCase.getLikeCountsForDiaries(diaryIds);
	}

	@Override
	public boolean isLikedByUser(String userId, Long diaryId) {
		return likeUseCase.isLikedByUser(userId, diaryId);
	}

	@Override
	public Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds) {
		return likeUseCase.getLikedDiaryIds(userId, diaryIds);
	}

	@Override
	public long countComments(Long diaryId) {
		return commentUseCase.countAllCommentsByDiaryId(diaryId);
	}
}
