package com.pikume.back.feed.application.port.out;

import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;

import java.util.List;
import java.util.Optional;

public interface LoadRecommendationForFeedPort {

	List<Long> getCachedFeed(String userId);

	void cacheFeed(String userId, List<Long> diaryIds);

	void invalidateCache(String userId);

	List<RecommendationScoreResult> getRecommendedDiaries(String userId, List<Long> allCandidateIds, List<Long> friendDiaryIds);

	void recordInteraction(String userId, String topic, String interactionType);

	Optional<String> getMetadataTopic(Long diaryId);
}
