package com.pikume.back.social.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryCommentEngagementUseCase {
	long queryActiveCommentCount(String viewerId, Long diaryId);

	Map<Long, Long> queryCommentCounts(List<Long> diaryIds);

	Set<Long> queryCommentedDiaryIds(String userId, List<Long> diaryIds);
}
