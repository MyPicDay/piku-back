package com.pikume.back.feed.application.port.out;

import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.readmodel.FeedEngagementView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadFeedEngagementPort {

	Map<Long, FeedEngagementView> loadEngagements(String currentUserId, List<Long> diaryIds);

	Map<String, FeedFriendStatus> loadFriendStatuses(String currentUserId, Set<String> targetUserIds);

	Map<Long, Long> loadLikeCounts(List<Long> diaryIds);

	Map<Long, Long> loadCommentCounts(List<Long> diaryIds);

	Set<Long> loadLikedDiaryIds(String currentUserId, List<Long> diaryIds);

	Set<Long> loadCommentedDiaryIds(String currentUserId, List<Long> diaryIds);
}
