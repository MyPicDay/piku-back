package store.piku.back.feed.adapter.out.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.feed.application.port.out.LoadRecommendationForFeedPort;
import store.piku.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import store.piku.back.recommendation.application.port.in.CacheFeedUseCase;
import store.piku.back.recommendation.application.port.in.GetRecommendationUseCase;
import store.piku.back.recommendation.application.port.in.ManageUserPreferenceUseCase;
import store.piku.back.recommendation.domain.ScoredDiary;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecommendationAdapterForFeed implements LoadRecommendationForFeedPort {

	private final CacheFeedUseCase cacheFeedUseCase;
	private final GetRecommendationUseCase getRecommendationUseCase;
	private final ManageUserPreferenceUseCase manageUserPreferenceUseCase;
	private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

	@Override
	public List<Long> getCachedFeed(String userId) {
		return cacheFeedUseCase.getCachedFeed(userId);
	}

	@Override
	public void cacheFeed(String userId, List<Long> diaryIds) {
		cacheFeedUseCase.cacheFeed(userId, diaryIds);
	}

	@Override
	public void invalidateCache(String userId) {
		cacheFeedUseCase.invalidateCache(userId);
	}

	@Override
	public List<ScoredDiary> getRecommendedDiaries(String userId, List<Long> allCandidateIds, List<Long> friendDiaryIds) {
		return getRecommendationUseCase.getRecommendedDiaries(userId, allCandidateIds, friendDiaryIds);
	}

	@Override
	public void recordInteraction(String userId, String topic, String interactionType) {
		manageUserPreferenceUseCase.recordInteraction(userId, topic, interactionType);
	}

	@Override
	public Optional<String> getMetadataTopic(Long diaryId) {
		return analyzeDiaryContentUseCase.getMetadata(diaryId)
				.map(meta -> meta.getPrimaryTopic());
	}
}
