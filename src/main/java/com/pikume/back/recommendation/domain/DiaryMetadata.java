package com.pikume.back.recommendation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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
	private String topics;

	@Column(name = "quality_score")
	private Double qualityScore;

	@Column(name = "analyzed_at")
	private LocalDateTime analyzedAt;

	@Builder
	public DiaryMetadata(Long diaryId, String primaryTopic, String topics, Double qualityScore) {
		this.diaryId = diaryId;
		this.primaryTopic = primaryTopic;
		this.topics = topics;
		this.qualityScore = qualityScore;
		this.analyzedAt = LocalDateTime.now();
	}

	public void updateAnalysis(String primaryTopic, String topics, Double qualityScore) {
		this.primaryTopic = primaryTopic;
		this.topics = topics;
		this.qualityScore = qualityScore;
		this.analyzedAt = LocalDateTime.now();
	}
}
