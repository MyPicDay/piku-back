package com.pikume.back.recommendation.application.port.in;

import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;

import java.util.List;

public interface ScoreDiaryCandidatesUseCase {

	List<RecommendationScoreResult> scoreDiaryCandidates(String userId, List<Long> candidateDiaryIds);
}
