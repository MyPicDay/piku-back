package com.pikume.back.recommendation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "diary_metadata", indexes = {
		@Index(name = "idx_diary_metadata_diary", columnList = "diary_id"),
		@Index(name = "idx_diary_metadata_topic", columnList = "primary_topic")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryMetadata extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "diary_id", nullable = false, unique = true)
	private Long diaryId;

	@Column(name = "primary_topic", length = 50)
	private String primaryTopic;

	@Column(columnDefinition = "TEXT")
	private TopicScores topics;

	@Column(name = "quality_score")
	private Double qualityScore;

	@Column(name = "analyzed_at")
	private LocalDateTime analyzedAt;

	private DiaryMetadata(
			Long diaryId,
			String primaryTopic,
			TopicScores topics,
			Double qualityScore,
			LocalDateTime analyzedAt
	) {
		this.diaryId = diaryId;
		this.primaryTopic = primaryTopic;
		this.topics = topics != null ? topics : TopicScores.empty();
		this.qualityScore = qualityScore;
		this.analyzedAt = analyzedAt;
	}

	public static DiaryMetadata create(
			Long diaryId,
			String primaryTopic,
			TopicScores topics,
			Double qualityScore,
			LocalDateTime analyzedAt
	) {
		return new DiaryMetadata(diaryId, primaryTopic, topics, qualityScore, analyzedAt);
	}

	public void updateAnalysis(
			String primaryTopic,
			TopicScores topics,
			Double qualityScore,
			LocalDateTime analyzedAt
	) {
		this.primaryTopic = primaryTopic;
		this.topics = topics != null ? topics : TopicScores.empty();
		this.qualityScore = qualityScore;
		this.analyzedAt = analyzedAt;
	}
}
