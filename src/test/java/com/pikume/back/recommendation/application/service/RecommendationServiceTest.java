package com.pikume.back.recommendation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.recommendation.application.port.in.ManageUserPreferenceUseCase;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.ScoredDiary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	@InjectMocks
	private RecommendationService recommendationService;

	@Mock
	private LoadDiaryMetadataPort loadDiaryMetadataPort;

	@Mock
	private ManageUserPreferenceUseCase userPreferenceUseCase;

	@Nested
	@DisplayName("calculateScore - 일기 스코어 계산")
	class CalculateScore {

		@Test
		@DisplayName("사용자 선호 토픽과 일치하면 높은 점수를 받는다")
		void matchingTopicHighScore() {
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(1L)
					.primaryTopic("travel")
					.qualityScore(0.8)
					.build();

			Map<String, Double> userAffinities = Map.of("travel", 0.9, "food", 0.3);

			double score = recommendationService.calculateScore(metadata, userAffinities, false);

			assertThat(score).isGreaterThan(0.5);
		}

		@Test
		@DisplayName("친구 여부는 score bonus가 아니라 feed composition 단계에서만 반영한다")
		void friendStatusDoesNotChangeScore() {
			DiaryMetadata metadata = metadataWithAnalyzedAt(1L, "daily", 0.5, LocalDateTime.now().minusHours(6));

			Map<String, Double> userAffinities = Map.of("daily", 0.5);

			double nonFriendScore = recommendationService.calculateScore(metadata, userAffinities, false);
			double friendScore = recommendationService.calculateScore(metadata, userAffinities, true);

			assertThat(friendScore).isEqualTo(nonFriendScore);
		}

		@Test
		@DisplayName("품질 점수가 높은 일기가 더 높은 점수를 받는다")
		void highQualityHighScore() {
			DiaryMetadata lowQuality = DiaryMetadata.builder()
					.diaryId(1L)
					.primaryTopic("travel")
					.qualityScore(0.2)
					.build();

			DiaryMetadata highQuality = DiaryMetadata.builder()
					.diaryId(2L)
					.primaryTopic("travel")
					.qualityScore(0.9)
					.build();

			Map<String, Double> userAffinities = Map.of("travel", 0.5);

			double lowScore = recommendationService.calculateScore(lowQuality, userAffinities, false);
			double highScore = recommendationService.calculateScore(highQuality, userAffinities, false);

			assertThat(highScore).isGreaterThan(lowScore);
		}

		@Test
		@DisplayName("최신성이 높은 일기가 더 높은 점수를 받는다")
		void recentDiaryHighScore() {
			DiaryMetadata recent = metadataWithAnalyzedAt(1L, "travel", 0.5, LocalDateTime.now().minusHours(2));
			DiaryMetadata stale = metadataWithAnalyzedAt(2L, "travel", 0.5, LocalDateTime.now().minusDays(7));

			Map<String, Double> userAffinities = Map.of("travel", 0.5);

			double recentScore = recommendationService.calculateScore(recent, userAffinities, false);
			double staleScore = recommendationService.calculateScore(stale, userAffinities, false);

			assertThat(recentScore).isGreaterThan(staleScore);
		}
	}

	@Nested
	@DisplayName("scoreAndSort - 점수 계산 및 정렬")
	class ScoreAndSort {

		@Test
		@DisplayName("점수 순으로 정렬된 추천 목록을 반환한다")
		void returnsSortedByScore() {
			DiaryMetadata meta1 = DiaryMetadata.builder()
					.diaryId(1L)
					.primaryTopic("travel")
					.qualityScore(0.9)
					.build();
			DiaryMetadata meta2 = DiaryMetadata.builder()
					.diaryId(2L)
					.primaryTopic("daily")
					.qualityScore(0.5)
					.build();

			List<ScoredDiary> result = recommendationService.scoreAndSort(
					List.of(meta1, meta2),
					Map.of("travel", 0.8),
					List.of());

			assertThat(result).hasSize(2);
			assertThat(result.get(0).getDiaryId()).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("getRecommendedDiaries - 추천 일기 목록 조회")
	class GetRecommendedDiaries {

		@Test
		@DisplayName("후보가 비어있으면 빈 리스트를 반환한다")
		void returnsEmptyForNoCandidates() {
			List<ScoredDiary> result = recommendationService.getRecommendedDiaries(
					"user-1", List.of(), List.of());

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("메타데이터가 있는 후보에 대해 스코어를 계산한다")
		void scoresWithMetadata() {
			DiaryMetadata meta = DiaryMetadata.builder()
					.diaryId(1L)
					.primaryTopic("travel")
					.qualityScore(0.8)
					.build();

			given(loadDiaryMetadataPort.findByDiaryIds(List.of(1L))).willReturn(List.of(meta));
			given(userPreferenceUseCase.getPreference("user-1")).willReturn(Optional.empty());

			List<ScoredDiary> result = recommendationService.getRecommendedDiaries(
					"user-1", List.of(1L), List.of());

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getDiaryId()).isEqualTo(1L);
			assertThat(result.get(0).getScore()).isGreaterThan(0);
		}
	}

	private DiaryMetadata metadataWithAnalyzedAt(Long diaryId, String topic, double qualityScore,
			LocalDateTime analyzedAt) {
		DiaryMetadata metadata = DiaryMetadata.builder()
				.diaryId(diaryId)
				.primaryTopic(topic)
				.qualityScore(qualityScore)
				.build();
		ReflectionTestUtils.setField(metadata, "analyzedAt", analyzedAt);
		return metadata;
	}
}
