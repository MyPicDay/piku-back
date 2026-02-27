package com.pikume.back.feed.application.port.out;

import org.springframework.data.domain.Pageable;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadSocialForFeedPort {

	boolean areFriends(String userId1, String userId2);

	List<String> getFriendIds(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo);

	FriendStatus getFriendshipStatus(String userId, String targetUserId);

	long getLikeCount(Long diaryId);

	Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds);

	boolean isLikedByUser(String userId, Long diaryId);

	Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds);

	long countComments(Long diaryId);
}
