package com.pikume.back.recommendation.application.port.out;

import java.util.List;

/**
 * 추천 캐시 Outbound Port
 */
public interface RecommendationCachePort {

	void cacheFeed(String userId, List<Long> diaryIds);

	List<Long> getCachedFeed(String userId);

	void invalidateCache(String userId);
}
