package store.piku.back.recommendation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationCacheServiceTest {

	@InjectMocks
	private RecommendationCacheService cacheService;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private ValueOperations<String, Object> valueOperations;

	private final String userId = "test-user-id";

	@Nested
	@DisplayName("cacheFeed - 피드 캐시 저장")
	class CacheFeed {

		@Test
		@DisplayName("피드 목록을 Redis에 캐시한다")
		void cacheFeedToRedis() {
			List<Long> diaryIds = List.of(1L, 2L, 3L);

			given(redisTemplate.opsForValue()).willReturn(valueOperations);

			cacheService.cacheFeed(userId, diaryIds);

			verify(valueOperations).set(eq("feed:" + userId), eq(diaryIds), anyLong(), eq(TimeUnit.MINUTES));
		}
	}

	@Nested
	@DisplayName("getCachedFeed - 캐시된 피드 조회")
	class GetCachedFeed {

		@Test
		@DisplayName("캐시된 피드가 있으면 반환한다")
		void returnCachedFeed() {
			List<Long> cachedIds = List.of(1L, 2L, 3L);

			given(redisTemplate.opsForValue()).willReturn(valueOperations);
			given(valueOperations.get("feed:" + userId)).willReturn(cachedIds);

			List<Long> result = cacheService.getCachedFeed(userId);

			assertThat(result).containsExactly(1L, 2L, 3L);
		}

		@Test
		@DisplayName("캐시가 없으면 빈 리스트를 반환한다")
		void returnEmptyWhenNoCache() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
			given(valueOperations.get("feed:" + userId)).willReturn(null);

			List<Long> result = cacheService.getCachedFeed(userId);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("invalidateCache - 캐시 무효화")
	class InvalidateCache {

		@Test
		@DisplayName("사용자 피드 캐시를 삭제한다")
		void deleteFeedCache() {
			cacheService.invalidateCache(userId);

			verify(redisTemplate).delete("feed:" + userId);
		}
	}
}
