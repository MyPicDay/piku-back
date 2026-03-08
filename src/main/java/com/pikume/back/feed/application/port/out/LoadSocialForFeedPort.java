package com.pikume.back.feed.application.port.out;

import java.util.List;

public interface LoadSocialForFeedPort {

	boolean areFriends(String userId1, String userId2);

	List<String> getFriendIds(String userId);

	long getLikeCount(Long diaryId);

	boolean isLikedByUser(String userId, Long diaryId);

	long countComments(Long diaryId);
}
