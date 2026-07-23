package com.pikume.back.recommendation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationScore")
class RecommendationScoreTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);

	@Test
	@DisplayName("주제 친화도, 품질과 72시간 최신성 감쇠를 현재 상수로 계산한다")
	void calculatesCurrentRecommendationFormula() {
		DiaryMetadata metadata = DiaryMetadata.create(
				1L,
				"travel",
				TopicScores.from(Map.of("travel", 0.6)),
				0.8,
				NOW.minusHours(72));

		RecommendationScore score = RecommendationScore.calculate(
				metadata,
				TopicAffinities.from(Map.of("travel", 0.9)),
				NOW);

		assertThat(score.value()).isCloseTo(0.7, org.assertj.core.data.Offset.offset(1.0e-12));
	}

	@Test
	@DisplayName("메타데이터가 없는 후보의 현재 기본 점수는 0.3이다")
	void usesCurrentDefaultMetadataScore() {
		assertThat(RecommendationScore.withoutMetadata().value()).isEqualTo(0.3);
	}
}
