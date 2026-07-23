package com.pikume.back.social.application.port.out;

import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import com.pikume.back.social.domain.like.Like;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoadDiaryLikesPort {
	Optional<Like> loadActiveLike(String userId, Long diaryId);

	Optional<Like> loadLikeState(String userId, Long diaryId);

	boolean activeLikeExists(String userId, Long diaryId);

	long countActiveLikes(Long diaryId);

	List<DiaryEngagementCount> loadActiveLikeCounts(List<Long> diaryIds);

	Set<Long> loadLikedDiaryIds(String userId, List<Long> diaryIds);
}
