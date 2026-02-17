package store.piku.back.feed.application.port.out;

import store.piku.back.recommendation.domain.ScoredDiary;

import java.util.List;
import java.util.Optional;

public interface LoadRecommendationForFeedPort {

	List<Long> getCachedFeed(String userId);

	void cacheFeed(String userId, List<Long> diaryIds);

	void invalidateCache(String userId);

	List<ScoredDiary> getRecommendedDiaries(String userId, List<Long> allCandidateIds, List<Long> friendDiaryIds);

	void recordInteraction(String userId, String topic, String interactionType);

	Optional<String> getMetadataTopic(Long diaryId);
}
