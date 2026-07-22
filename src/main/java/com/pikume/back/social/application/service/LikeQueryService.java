package com.pikume.back.social.application.service;

import com.pikume.back.social.application.dto.LikeResult;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.in.QueryDiaryLikeEngagementUseCase;
import com.pikume.back.social.application.port.out.LoadDiaryLikesPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeQueryService implements QueryDiaryLikeEngagementUseCase {

	private final LoadDiaryLikesPort loadDiaryLikesPort;
	private final ResolveInteractionDiaryPort resolveInteractionDiaryPort;

	@Override
	public LikeResult queryLikeStatus(String userId, Long diaryId) {
		verifyVisible(diaryId, userId);
		return new LikeResult(
				diaryId,
				loadDiaryLikesPort.countActiveLikes(diaryId),
				userId != null && loadDiaryLikesPort.activeLikeExists(userId, diaryId));
	}

	@Override
	public long queryVisibleLikeCount(String userId, Long diaryId) {
		verifyVisible(diaryId, userId);
		return loadDiaryLikesPort.countActiveLikes(diaryId);
	}

	@Override
	public long queryLikeCount(Long diaryId) {
		return loadDiaryLikesPort.countActiveLikes(diaryId);
	}

	@Override
	public boolean isLikedByUser(String userId, Long diaryId) {
		return userId != null && loadDiaryLikesPort.activeLikeExists(userId, diaryId);
	}

	@Override
	public Map<Long, Long> queryLikeCounts(List<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}
		return loadDiaryLikesPort.loadActiveLikeCounts(diaryIds).stream()
				.collect(Collectors.toMap(DiaryEngagementCount::diaryId, DiaryEngagementCount::count));
	}

	@Override
	public Set<Long> queryLikedDiaryIds(String userId, List<Long> diaryIds) {
		if (userId == null || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return loadDiaryLikesPort.loadLikedDiaryIds(userId, diaryIds);
	}

	private void verifyVisible(Long diaryId, String viewerId) {
		if (!resolveInteractionDiaryPort.visibleDiaryExists(diaryId, viewerId)) {
			throw new SocialException(SocialErrorCode.DIARY_NOT_FOUND);
		}
	}
}
