package com.pikume.back.recommendation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TopicScores")
class TopicScoresTest {

	@Test
	@DisplayName("같은 주제별 값을 가진 점수는 같은 값 객체다")
	void comparesByTopicValues() {
		assertThat(TopicScores.from(Map.of("food", 0.9)))
				.isEqualTo(TopicScores.from(Map.of("food", 0.9)));
	}
}
