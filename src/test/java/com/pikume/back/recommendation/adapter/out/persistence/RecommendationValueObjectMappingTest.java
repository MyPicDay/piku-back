package com.pikume.back.recommendation.adapter.out.persistence;

import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.InteractionType;
import com.pikume.back.recommendation.domain.TopicScores;
import com.pikume.back.recommendation.domain.UserPreference;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Recommendation value object JPA mapping")
class RecommendationValueObjectMappingTest {

	@Autowired
	private DiaryMetadataJpaRepository diaryMetadataJpaRepository;

	@Autowired
	private UserPreferenceJpaRepository userPreferenceJpaRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("주제 점수를 기존 topics TEXT JSON 열에 저장하고 다시 읽는다")
	void mapsTopicScoresToExistingJsonColumn() {
		DiaryMetadata metadata = DiaryMetadata.create(
				1L,
				"travel",
				TopicScores.from(Map.of("travel", 0.6)),
				0.8,
				LocalDateTime.of(2026, 7, 23, 12, 0));
		diaryMetadataJpaRepository.saveAndFlush(metadata);
		entityManager.clear();

		DiaryMetadata reloaded = diaryMetadataJpaRepository.findByDiaryId(1L).orElseThrow();

		assertThat(reloaded.getTopics().values()).containsEntry("travel", 0.6);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT topics FROM diary_metadata WHERE diary_id = 1",
				String.class)).contains("\"travel\":0.6");
	}

	@Test
	@DisplayName("주제 친화도를 기존 topic_affinities TEXT JSON 열에 저장하고 다시 읽는다")
	void mapsTopicAffinitiesToExistingJsonColumn() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 23, 12, 0);
		UserPreference preference = UserPreference.create("user-1", now.minusHours(1));
		preference.recordInteraction("food", InteractionType.LIKE, now);
		userPreferenceJpaRepository.saveAndFlush(preference);
		entityManager.clear();

		UserPreference reloaded = userPreferenceJpaRepository.findByUserId("user-1").orElseThrow();

		assertThat(reloaded.getTopicAffinities().values()).containsEntry("food", 0.3);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT topic_affinities FROM user_preference WHERE user_id = 'user-1'",
				String.class)).contains("\"food\":0.3");
	}
}
