package com.pikume.back.recommendation.application.service;

import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;
import com.pikume.back.recommendation.application.port.in.QueryUserTopicAffinitiesUseCase;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecommendationClockPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.TopicScores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);

	@Mock
	private LoadDiaryMetadataPort loadDiaryMetadataPort;

	@Mock
	private QueryUserTopicAffinitiesUseCase queryUserTopicAffinitiesUseCase;

	@Mock
	private RecommendationClockPort recommendationClockPort;

	private RecommendationService recommendationService;

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationService(
				loadDiaryMetadataPort,
				queryUserTopicAffinitiesUseCase,
				recommendationClockPort);
	}

	@Nested
	@DisplayName("calculateScore")
	class CalculateScore {

		@Test
		@DisplayName("사용자 선호 주제와 일치하면 현재 공식에 따라 높은 점수를 받는다")
		void matchingTopicHighScore() {
			given(recommendationClockPort.now()).willReturn(NOW);
			DiaryMetadata metadata = metadata(1L, "travel", 0.8, NOW.minusHours(6));

			double score = recommendationService.calculateScore(
					metadata,
					Map.of("travel", 0.9, "food", 0.3));

			assertThat(score).isGreaterThan(0.5);
		}

		@Test
		@DisplayName("품질 점수가 높은 일기가 더 높은 점수를 받는다")
		void highQualityHighScore() {
			given(recommendationClockPort.now()).willReturn(NOW);
			DiaryMetadata lowQuality = metadata(1L, "travel", 0.2, NOW.minusHours(6));
			DiaryMetadata highQuality = metadata(2L, "travel", 0.9, NOW.minusHours(6));

			double lowScore = recommendationService.calculateScore(lowQuality, Map.of("travel", 0.5));
			double highScore = recommendationService.calculateScore(highQuality, Map.of("travel", 0.5));

			assertThat(highScore).isGreaterThan(lowScore);
		}

		@Test
		@DisplayName("기준 시각에 더 가까운 분석 결과가 더 높은 점수를 받는다")
		void recentDiaryHighScore() {
			given(recommendationClockPort.now()).willReturn(NOW);
			DiaryMetadata recent = metadata(1L, "travel", 0.5, NOW.minusHours(2));
			DiaryMetadata stale = metadata(2L, "travel", 0.5, NOW.minusDays(7));

			double recentScore = recommendationService.calculateScore(recent, Map.of("travel", 0.5));
			double staleScore = recommendationService.calculateScore(stale, Map.of("travel", 0.5));

			assertThat(recentScore).isGreaterThan(staleScore);
		}
	}

	@Nested
	@DisplayName("scoreDiaryCandidates")
	class ScoreDiaryCandidates {

		@Test
		@DisplayName("후보가 비어 있으면 빈 목록을 반환한다")
		void returnsEmptyForNoCandidates() {
			assertThat(recommendationService.scoreDiaryCandidates("user-1", List.of())).isEmpty();
		}

		@Test
		@DisplayName("메타데이터가 없는 후보는 현재 기본 점수 0.3을 사용한다")
		void usesDefaultScoreForMissingMetadata() {
			given(loadDiaryMetadataPort.loadByDiaryIds(List.of(1L))).willReturn(List.of());
			given(queryUserTopicAffinitiesUseCase.queryUserTopicAffinities("user-1")).willReturn(Map.of());

			List<RecommendationScoreResult> result =
					recommendationService.scoreDiaryCandidates("user-1", List.of(1L));

			assertThat(result).containsExactly(new RecommendationScoreResult(1L, 0.3));
		}

		@Test
		@DisplayName("같은 점수의 후보는 입력 순서를 유지한다")
		void keepsInputOrderForEqualScores() {
			given(recommendationClockPort.now()).willReturn(NOW);
			DiaryMetadata first = metadata(1L, "daily", 0.5, NOW.minusHours(1));
			DiaryMetadata second = metadata(2L, "daily", 0.5, NOW.minusHours(1));
			given(loadDiaryMetadataPort.loadByDiaryIds(List.of(2L, 1L)))
					.willReturn(List.of(first, second));
			given(queryUserTopicAffinitiesUseCase.queryUserTopicAffinities("user-1"))
					.willReturn(Map.of("daily", 0.5));

			List<RecommendationScoreResult> result =
					recommendationService.scoreDiaryCandidates("user-1", List.of(2L, 1L));

			assertThat(result).extracting(RecommendationScoreResult::diaryId)
					.containsExactly(2L, 1L);
		}
	}

	private DiaryMetadata metadata(
			Long diaryId,
			String topic,
			double qualityScore,
			LocalDateTime analyzedAt
	) {
		return DiaryMetadata.create(
				diaryId,
				topic,
				TopicScores.from(Map.of(topic, 0.6)),
				qualityScore,
				analyzedAt);
	}
}
