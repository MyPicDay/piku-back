package com.pikume.back.recommendation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.recommendation.application.port.out.RecommendationCachePort;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationCacheServiceTest {

	@InjectMocks
	private RecommendationCacheService cacheService;

	@Mock
	private RecommendationCachePort recommendationCachePort;

	private final String userId = "test-user-id";

	@Nested
	@DisplayName("cacheFeed - 피드 캐시 저장")
	class CacheFeed {

		@Test
		@DisplayName("피드 목록을 캐시 포트에 저장한다")
		void cacheFeedToPort() {
			List<Long> diaryIds = List.of(1L, 2L, 3L);

			cacheService.cacheFeed(userId, diaryIds);

			verify(recommendationCachePort).cacheFeed(userId, diaryIds);
		}
	}

	@Nested
	@DisplayName("getCachedFeed - 캐시된 피드 조회")
	class GetCachedFeed {

		@Test
		@DisplayName("캐시된 피드가 있으면 반환한다")
		void returnCachedFeed() {
			List<Long> cachedIds = List.of(1L, 2L, 3L);

			given(recommendationCachePort.getCachedFeed(userId)).willReturn(cachedIds);

			List<Long> result = cacheService.getCachedFeed(userId);

			assertThat(result).containsExactly(1L, 2L, 3L);
		}

		@Test
		@DisplayName("캐시가 없으면 빈 리스트를 반환한다")
		void returnEmptyWhenNoCache() {
			given(recommendationCachePort.getCachedFeed(userId)).willReturn(Collections.emptyList());

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

			verify(recommendationCachePort).invalidateCache(userId);
		}
	}
}
