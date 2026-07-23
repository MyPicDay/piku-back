package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.LikeResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryDiaryLikeEngagementUseCase {
	LikeResult queryLikeStatus(String userId, Long diaryId);

	long queryVisibleLikeCount(String userId, Long diaryId);

	long queryLikeCount(Long diaryId);

	boolean isLikedByUser(String userId, Long diaryId);

	Map<Long, Long> queryLikeCounts(List<Long> diaryIds);

	Set<Long> queryLikedDiaryIds(String userId, List<Long> diaryIds);
}
