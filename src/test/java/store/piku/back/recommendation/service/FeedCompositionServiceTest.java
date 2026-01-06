package store.piku.back.recommendation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.recommendation.dto.ScoredDiary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeedCompositionServiceTest {

	@InjectMocks
	private FeedCompositionService feedCompositionService;

	@Mock
	private RecommendationService recommendationService;

	private final String userId = "test-user-id";

	@Nested
	@DisplayName("composeFeed - 피드 구성")
	class ComposeFeed {

		@Test
		@DisplayName("고정 슬롯에 친구 일기를 우선 배치한다")
		void friendDiariesInFixedSlots() {
			List<Long> friendDiaryIds = List.of(1L, 2L);
			List<Long> publicDiaryIds = List.of(3L, 4L, 5L);

			List<ScoredDiary> scored = List.of(
					new ScoredDiary(1L, 0.9),
					new ScoredDiary(2L, 0.8),
					new ScoredDiary(3L, 0.7),
					new ScoredDiary(4L, 0.6),
					new ScoredDiary(5L, 0.5));

			given(recommendationService.getRecommendedDiaries(eq(userId), anyList(), anyList()))
					.willReturn(scored);

			List<Long> result = feedCompositionService.composeFeed(userId, friendDiaryIds, publicDiaryIds, 5);

			assertThat(result).hasSize(5);
			assertThat(result.subList(0, 2)).containsExactly(1L, 2L);
		}

		@Test
		@DisplayName("변동 슬롯에 추천 점수순 일기를 배치한다")
		void recommendedDiariesInVariableSlots() {
			List<Long> friendDiaryIds = List.of(1L);
			List<Long> publicDiaryIds = List.of(2L, 3L, 4L, 5L);

			List<ScoredDiary> scored = List.of(
					new ScoredDiary(1L, 0.9),
					new ScoredDiary(3L, 0.85),
					new ScoredDiary(2L, 0.7),
					new ScoredDiary(4L, 0.6),
					new ScoredDiary(5L, 0.5));

			given(recommendationService.getRecommendedDiaries(eq(userId), anyList(), anyList()))
					.willReturn(scored);

			List<Long> result = feedCompositionService.composeFeed(userId, friendDiaryIds, publicDiaryIds, 5);

			assertThat(result).hasSize(5);
			assertThat(result.get(0)).isEqualTo(1L);
		}

		@Test
		@DisplayName("친구 일기가 없으면 추천 일기만으로 피드를 구성한다")
		void noFriendDiariesOnlyRecommended() {
			List<Long> friendDiaryIds = List.of();
			List<Long> publicDiaryIds = List.of(1L, 2L, 3L);

			List<ScoredDiary> scored = List.of(
					new ScoredDiary(2L, 0.9),
					new ScoredDiary(1L, 0.7),
					new ScoredDiary(3L, 0.5));

			given(recommendationService.getRecommendedDiaries(eq(userId), anyList(), anyList()))
					.willReturn(scored);

			List<Long> result = feedCompositionService.composeFeed(userId, friendDiaryIds, publicDiaryIds, 3);

			assertThat(result).hasSize(3);
			assertThat(result.get(0)).isEqualTo(2L);
		}

		@Test
		@DisplayName("요청 크기보다 적은 일기가 있으면 가능한 만큼만 반환한다")
		void lessThanRequestedSize() {
			List<Long> friendDiaryIds = List.of(1L);
			List<Long> publicDiaryIds = List.of(2L);

			List<ScoredDiary> scored = List.of(
					new ScoredDiary(1L, 0.9),
					new ScoredDiary(2L, 0.7));

			given(recommendationService.getRecommendedDiaries(eq(userId), anyList(), anyList()))
					.willReturn(scored);

			List<Long> result = feedCompositionService.composeFeed(userId, friendDiaryIds, publicDiaryIds, 10);

			assertThat(result).hasSize(2);
		}
	}
}
