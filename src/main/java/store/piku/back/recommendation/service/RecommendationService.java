package store.piku.back.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.recommendation.dto.ScoredDiary;
import store.piku.back.recommendation.entity.DiaryMetadata;
import store.piku.back.recommendation.entity.UserPreference;
import store.piku.back.recommendation.repository.DiaryMetadataRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

	private final DiaryMetadataRepository diaryMetadataRepository;
	private final UserPreferenceService userPreferenceService;

	private static final double TOPIC_WEIGHT = 0.4;
	private static final double QUALITY_WEIGHT = 0.3;
	private static final double RECENCY_WEIGHT = 0.2;
	private static final double FRIEND_BONUS = 0.15;

	public double calculateScore(DiaryMetadata metadata, Map<String, Double> userAffinities, boolean isFriend) {
		double topicScore = 0.0;
		double qualityScore = metadata.getQualityScore() != null ? metadata.getQualityScore() : 0.5;

		if (metadata.getPrimaryTopic() != null) {
			topicScore = userAffinities.getOrDefault(metadata.getPrimaryTopic(), 0.1);
		}

		double baseScore = (TOPIC_WEIGHT * topicScore)
				+ (QUALITY_WEIGHT * qualityScore)
				+ (RECENCY_WEIGHT * 0.5);

		if (isFriend) {
			baseScore += FRIEND_BONUS;
		}

		return Math.min(1.0, baseScore);
	}

	public List<ScoredDiary> scoreAndSort(List<DiaryMetadata> metadataList,
			Map<String, Double> userAffinities,
			List<Long> friendDiaryIds) {
		Set<Long> friendSet = new HashSet<>(friendDiaryIds);

		return metadataList.stream()
				.map(meta -> {
					boolean isFriend = friendSet.contains(meta.getDiaryId());
					double score = calculateScore(meta, userAffinities, isFriend);
					return new ScoredDiary(meta.getDiaryId(), score);
				})
				.sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ScoredDiary> getRecommendedDiaries(String userId, List<Long> candidateDiaryIds,
			List<Long> friendDiaryIds) {
		if (candidateDiaryIds == null || candidateDiaryIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<DiaryMetadata> metadataList = diaryMetadataRepository.findByDiaryIds(candidateDiaryIds);

		Map<String, Double> userAffinities = getUserAffinities(userId);

		return scoreAndSort(metadataList, userAffinities, friendDiaryIds);
	}

	private Map<String, Double> getUserAffinities(String userId) {
		if (userId == null) {
			return Collections.emptyMap();
		}

		return userPreferenceService.getPreference(userId)
				.map(pref -> userPreferenceService.parseAffinities(pref.getTopicAffinities()))
				.orElse(Collections.emptyMap());
	}
}
