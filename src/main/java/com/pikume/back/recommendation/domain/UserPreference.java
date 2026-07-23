package com.pikume.back.recommendation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference", indexes = {
		@Index(name = "idx_user_preference_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true, length = 36)
	private String userId;

	@Column(name = "topic_affinities", columnDefinition = "TEXT")
	private TopicAffinities topicAffinities;

	@Column(name = "last_updated_at")
	private LocalDateTime lastUpdatedAt;

	private UserPreference(String userId, TopicAffinities topicAffinities, LocalDateTime lastUpdatedAt) {
		this.userId = userId;
		this.topicAffinities = topicAffinities != null ? topicAffinities : TopicAffinities.empty();
		this.lastUpdatedAt = lastUpdatedAt;
	}

	public static UserPreference create(String userId, LocalDateTime createdAt) {
		return new UserPreference(userId, TopicAffinities.empty(), createdAt);
	}

	public void recordInteraction(String topic, InteractionType interactionType, LocalDateTime recordedAt) {
		this.topicAffinities = topicAffinities.record(topic, interactionType);
		this.lastUpdatedAt = recordedAt;
	}
}
