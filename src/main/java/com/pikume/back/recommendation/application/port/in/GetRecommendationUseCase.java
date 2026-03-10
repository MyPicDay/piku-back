package com.pikume.back.recommendation.application.port.in;

import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;

import java.util.List;

/**
 * 추천 스코어링 Inbound Port
 */
public interface GetRecommendationUseCase {

	List<RecommendationScoreResult> getRecommendedDiaries(String userId, List<Long> candidateDiaryIds,
			List<Long> friendDiaryIds);
}
