package store.piku.back.recommendation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

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
	private String topicAffinities;

	@Column(name = "last_updated_at")
	private LocalDateTime lastUpdatedAt;

	@Builder
	public UserPreference(String userId, String topicAffinities) {
		this.userId = userId;
		this.topicAffinities = topicAffinities != null ? topicAffinities : "{}";
		this.lastUpdatedAt = LocalDateTime.now();
	}

	public void updateTopicAffinities(String topicAffinities) {
		this.topicAffinities = topicAffinities;
		this.lastUpdatedAt = LocalDateTime.now();
	}
}
