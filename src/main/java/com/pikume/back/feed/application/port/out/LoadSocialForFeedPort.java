package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedFriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadSocialForFeedPort {

	boolean areFriends(String userId1, String userId2);

	List<String> getFriendIds(String userId);

	long getLikeCount(Long diaryId);

	boolean isLikedByUser(String userId, Long diaryId);

	long countComments(String viewerId, Long diaryId);

	Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds);

	Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds);

	Map<Long, Long> getCommentCountsForDiaries(List<Long> diaryIds);

	Set<Long> getCommentedDiaryIds(String userId, List<Long> diaryIds);

	Map<String, FeedFriendStatus> getFriendStatuses(String currentUserId, Set<String> targetUserIds);
}
