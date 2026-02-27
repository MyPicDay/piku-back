package com.pikume.back.recommendation.application.port.in;

import java.util.List;

/**
 * 피드 캐시 관리 Inbound Port
 */
public interface CacheFeedUseCase {

	void cacheFeed(String userId, List<Long> diaryIds);

	List<Long> getCachedFeed(String userId);

	void invalidateCache(String userId);
}
