package store.piku.back.recommendation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.piku.back.recommendation.application.port.in.CacheFeedUseCase;
import store.piku.back.recommendation.application.port.out.RecommendationCachePort;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationCacheService implements CacheFeedUseCase {

	private final RecommendationCachePort recommendationCachePort;

	@Override
	public void cacheFeed(String userId, List<Long> diaryIds) {
		recommendationCachePort.cacheFeed(userId, diaryIds);
		log.debug("피드 캐시 저장 - userId: {}, count: {}", userId, diaryIds.size());
	}

	@Override
	public List<Long> getCachedFeed(String userId) {
		List<Long> cached = recommendationCachePort.getCachedFeed(userId);
		if (cached.isEmpty()) {
			log.debug("피드 캐시 미스 - userId: {}", userId);
		} else {
			log.debug("피드 캐시 히트 - userId: {}", userId);
		}
		return cached;
	}

	@Override
	public void invalidateCache(String userId) {
		recommendationCachePort.invalidateCache(userId);
		log.debug("피드 캐시 무효화 - userId: {}", userId);
	}
}
