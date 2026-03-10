package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedCompositionService")
class FeedCompositionServiceTest {

	@InjectMocks
	private FeedCompositionService feedCompositionService;

	@Mock
	private LoadRecommendationForFeedPort loadRecommendationForFeedPort;

	@Test
	@DisplayName("고정 슬롯 수는 친구 후보 수가 아니라 요청 feed 크기를 기준으로 계산한다")
	void fixedSlotUsesRequestedFeedSize() {
		List<Long> friendDiaryIds = List.of(1L, 2L, 3L, 4L);
		List<Long> publicDiaryIds = List.of(5L, 6L);
		List<RecommendationScoreResult> scoredDiaries = List.of(
				new RecommendationScoreResult(1L, 0.95),
				new RecommendationScoreResult(5L, 0.90),
				new RecommendationScoreResult(6L, 0.85),
				new RecommendationScoreResult(2L, 0.20),
				new RecommendationScoreResult(3L, 0.15),
				new RecommendationScoreResult(4L, 0.10));

		given(loadRecommendationForFeedPort.getRecommendedDiaries("viewer", List.of(1L, 2L, 3L, 4L, 5L, 6L), friendDiaryIds))
				.willReturn(scoredDiaries);

		List<Long> result = feedCompositionService.composeFeed("viewer", friendDiaryIds, publicDiaryIds, 2);

		assertThat(result).containsExactly(1L, 5L);
	}
}
