package com.pikume.back.feed.adapter.out.social;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.social.application.port.in.CommentUseCase;
import com.pikume.back.social.application.port.in.FriendUseCase;
import com.pikume.back.social.application.port.in.LikeUseCase;

import java.util.List;

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
	public long countComments(Long diaryId) {
		return commentUseCase.countAllCommentsByDiaryId(diaryId);
	}
}
