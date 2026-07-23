package com.pikume.back.recommendation.application.dto;

import java.util.Map;

public record DiaryContentAnalysis(
		String primaryTopic,
		Map<String, Double> topicScores,
		double qualityScore
) {
}
