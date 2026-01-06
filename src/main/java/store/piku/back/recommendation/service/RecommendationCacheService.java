package store.piku.back.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationCacheService {

	private final RedisTemplate<String, Object> redisTemplate;

	private static final String FEED_CACHE_PREFIX = "feed:";
	private static final long CACHE_TTL_MINUTES = 30;

	public void cacheFeed(String userId, List<Long> diaryIds) {
		String key = FEED_CACHE_PREFIX + userId;
		redisTemplate.opsForValue().set(key, diaryIds, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
		log.debug("피드 캐시 저장 - userId: {}, count: {}", userId, diaryIds.size());
	}

	@SuppressWarnings("unchecked")
	public List<Long> getCachedFeed(String userId) {
		String key = FEED_CACHE_PREFIX + userId;
		Object cached = redisTemplate.opsForValue().get(key);

		if (cached == null) {
			log.debug("피드 캐시 미스 - userId: {}", userId);
			return Collections.emptyList();
		}

		log.debug("피드 캐시 히트 - userId: {}", userId);
		return (List<Long>) cached;
	}

	public void invalidateCache(String userId) {
		String key = FEED_CACHE_PREFIX + userId;
		redisTemplate.delete(key);
		log.debug("피드 캐시 무효화 - userId: {}", userId);
	}
}
