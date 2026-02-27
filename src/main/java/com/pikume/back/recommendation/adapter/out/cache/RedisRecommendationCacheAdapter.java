package com.pikume.back.recommendation.adapter.out.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.pikume.back.recommendation.application.port.out.RecommendationCachePort;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisRecommendationCacheAdapter implements RecommendationCachePort {

	private final RedisTemplate<String, Object> redisTemplate;

	private static final String FEED_CACHE_PREFIX = "feed:";
	private static final long CACHE_TTL_MINUTES = 30;

	@Override
	public void cacheFeed(String userId, List<Long> diaryIds) {
		String key = FEED_CACHE_PREFIX + userId;
		redisTemplate.opsForValue().set(key, diaryIds, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getCachedFeed(String userId) {
		String key = FEED_CACHE_PREFIX + userId;
		Object cached = redisTemplate.opsForValue().get(key);

		if (cached == null) {
			return Collections.emptyList();
		}

		return (List<Long>) cached;
	}

	@Override
	public void invalidateCache(String userId) {
		String key = FEED_CACHE_PREFIX + userId;
		redisTemplate.delete(key);
	}
}
