package com.pikume.back.recommendation.domain;

import java.time.Duration;
import java.time.LocalDateTime;

public record RecommendationScore(double value) {

	private static final double TOPIC_WEIGHT = 0.4;
	private static final double QUALITY_WEIGHT = 0.3;
	private static final double RECENCY_WEIGHT = 0.2;
	private static final double DEFAULT_TOPIC_SCORE = 0.1;
	private static final double DEFAULT_QUALITY_SCORE = 0.5;
	private static final double DEFAULT_METADATA_SCORE = 0.3;
	private static final double DEFAULT_RECENCY_SCORE = 0.5;
	private static final double RECENCY_HALF_LIFE_HOURS = 72.0;

	public static RecommendationScore calculate(
			DiaryMetadata metadata,
			TopicAffinities userAffinities,
			LocalDateTime now
	) {
		double topicScore = metadata.getPrimaryTopic() == null
				? 0.0
				: userAffinities.scoreFor(metadata.getPrimaryTopic(), DEFAULT_TOPIC_SCORE);
		double qualityScore = metadata.getQualityScore() != null
				? metadata.getQualityScore()
				: DEFAULT_QUALITY_SCORE;
		double baseScore = TOPIC_WEIGHT * topicScore
				+ QUALITY_WEIGHT * qualityScore
				+ RECENCY_WEIGHT * recencyScore(metadata, now);
		return new RecommendationScore(Math.min(1.0, baseScore));
	}

	public static RecommendationScore withoutMetadata() {
		return new RecommendationScore(DEFAULT_METADATA_SCORE);
	}

	private static double recencyScore(DiaryMetadata metadata, LocalDateTime now) {
		LocalDateTime referenceTime = metadata.getAnalyzedAt() != null
				? metadata.getAnalyzedAt()
				: metadata.getCreatedAt();
		if (referenceTime == null) {
			return DEFAULT_RECENCY_SCORE;
		}
		long ageHours = Math.max(0L, Duration.between(referenceTime, now).toHours());
		return 1.0 / (1.0 + ageHours / RECENCY_HALF_LIFE_HOURS);
	}
}
