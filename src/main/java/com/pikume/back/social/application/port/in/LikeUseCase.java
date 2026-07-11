package com.pikume.back.social.application.port.in;

import com.pikume.back.social.application.dto.LikeResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LikeUseCase {

	LikeResult addLike(String userId, Long diaryId);

	LikeResult removeLike(String userId, Long diaryId);

	LikeResult getLikeStatus(String userId, Long diaryId);

	long getLikeCount(String userId, Long diaryId);

	long getLikeCount(Long diaryId);

	boolean isLikedByUser(String userId, Long diaryId);

	Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds);

	Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds);
}
