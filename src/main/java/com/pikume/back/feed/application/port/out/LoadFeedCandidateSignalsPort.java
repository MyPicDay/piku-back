package com.pikume.back.feed.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LoadFeedCandidateSignalsPort {

	Map<Long, Long> loadLikeCounts(List<Long> diaryIds);

	Map<Long, Long> loadCommentCounts(List<Long> diaryIds);

	Set<Long> loadLikedDiaryIds(String currentUserId, List<Long> diaryIds);

	Set<Long> loadCommentedDiaryIds(String currentUserId, List<Long> diaryIds);
}
