package com.pikume.back.recommendation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TopicAffinities")
class TopicAffinitiesTest {

	@Test
	@DisplayName("상호작용 가중치를 누적하고 친화도는 1.0을 넘지 않는다")
	void accumulatesInteractionWeightWithinMaximum() {
		TopicAffinities affinities = TopicAffinities.from(Map.of("travel", 0.9));

		TopicAffinities updated = affinities.record("travel", InteractionType.CLICK);

		assertThat(updated.scoreFor("travel", 0.0)).isEqualTo(1.0);
		assertThat(affinities.scoreFor("travel", 0.0)).isEqualTo(0.9);
	}

	@Test
	@DisplayName("기존 문자열 상호작용 가중치를 유지한다")
	void preservesInteractionWeights() {
		assertThat(InteractionType.from("LIKE").weight()).isEqualTo(0.3);
		assertThat(InteractionType.from("view").weight()).isEqualTo(0.1);
		assertThat(InteractionType.from("CLICK").weight()).isEqualTo(0.15);
		assertThat(InteractionType.from("unknown").weight()).isEqualTo(0.05);
	}

	@Test
	@DisplayName("같은 주제별 값을 가진 친화도는 같은 값 객체다")
	void comparesByTopicValues() {
		assertThat(TopicAffinities.from(Map.of("travel", 0.6)))
				.isEqualTo(TopicAffinities.from(Map.of("travel", 0.6)));
	}
}
