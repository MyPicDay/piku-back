package com.pikume.back.social.application.port.out;

import com.pikume.back.social.application.readmodel.DiaryEngagementCount;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface LoadCommentEngagementPort {
	long countActiveComments(Long diaryId);

	List<DiaryEngagementCount> loadActiveCommentCounts(Collection<Long> diaryIds);

	Set<Long> loadCommentedDiaryIds(String userId, Collection<Long> diaryIds);
}
