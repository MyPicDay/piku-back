package store.piku.back.recommendation.application.port.in;

import store.piku.back.recommendation.domain.DiaryMetadata;
import store.piku.back.recommendation.domain.ScoredDiary;

import java.util.List;
import java.util.Map;

/**
 * 추천 스코어링 Inbound Port
 */
public interface GetRecommendationUseCase {

	double calculateScore(DiaryMetadata metadata, Map<String, Double> userAffinities, boolean isFriend);

	List<ScoredDiary> scoreAndSort(List<DiaryMetadata> metadataList,
			Map<String, Double> userAffinities, List<Long> friendDiaryIds);

	List<ScoredDiary> getRecommendedDiaries(String userId, List<Long> candidateDiaryIds,
			List<Long> friendDiaryIds);
}
