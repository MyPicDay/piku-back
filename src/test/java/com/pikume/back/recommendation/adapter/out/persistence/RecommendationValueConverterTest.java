package com.pikume.back.recommendation.adapter.out.persistence;

import com.pikume.back.recommendation.domain.TopicAffinities;
import com.pikume.back.recommendation.domain.TopicScores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recommendation JSON value converters")
class RecommendationValueConverterTest {

	@Test
	@DisplayName("주제 친화도를 기존 TEXT JSON 형식으로 왕복 변환한다")
	void convertsTopicAffinities() {
		TopicAffinitiesConverter converter = new TopicAffinitiesConverter();

		String json = converter.convertToDatabaseColumn(
				TopicAffinities.from(Map.of("travel", 0.6)));

		assertThat(converter.convertToEntityAttribute(json).values())
				.containsExactlyEntriesOf(Map.of("travel", 0.6));
	}

	@Test
	@DisplayName("손상된 친화도 JSON은 현재 의미대로 빈 값으로 복구한다")
	void recoversMalformedAffinitiesAsEmpty() {
		assertThat(new TopicAffinitiesConverter()
				.convertToEntityAttribute("{broken")
				.values()).isEmpty();
	}

	@Test
	@DisplayName("주제 점수를 기존 TEXT JSON 형식으로 왕복 변환한다")
	void convertsTopicScores() {
		TopicScoresConverter converter = new TopicScoresConverter();

		String json = converter.convertToDatabaseColumn(
				TopicScores.from(Map.of("food", 0.9)));

		assertThat(converter.convertToEntityAttribute(json).values())
				.containsExactlyEntriesOf(Map.of("food", 0.9));
	}
}
