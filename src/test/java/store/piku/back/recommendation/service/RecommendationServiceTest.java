package store.piku.back.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.recommendation.dto.ScoredDiary;
import store.piku.back.recommendation.entity.DiaryMetadata;
import store.piku.back.recommendation.entity.UserPreference;
import store.piku.back.recommendation.repository.DiaryMetadataRepository;
import store.piku.back.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	@InjectMocks
	private RecommendationService recommendationService;

	@Mock
	private DiaryRepository diaryRepository;

	@Mock
	private DiaryMetadataRepository diaryMetadataRepository;

	@Mock
	private UserPreferenceService userPreferenceService;

	private User owner;
	private final String userId = "test-user-id";

	@BeforeEach
	void setUp() {
		owner = new User("owner-id", "owner@test.com", "password", "owner", null);
	}

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
		@DisplayName("친구 콘텐츠는 추가 점수 보너스를 받는다")
		void friendContentBonus() {
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(1L)
					.primaryTopic("daily")
					.qualityScore(0.5)
					.build();

			Map<String, Double> userAffinities = Map.of("daily", 0.5);

			double nonFriendScore = recommendationService.calculateScore(metadata, userAffinities, false);
			double friendScore = recommendationService.calculateScore(metadata, userAffinities, true);

			assertThat(friendScore).isGreaterThan(nonFriendScore);
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
}
