package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.recommendation.application.dto.RecommendationScoreResult;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCompositionService {

	private final LoadRecommendationForFeedPort loadRecommendationForFeedPort;

	private static final double FIXED_SLOT_RATIO = 0.3;

	public List<Long> composeFeed(String userId, List<Long> friendDiaryIds,
			List<Long> publicDiaryIds, int requestedSize) {
		List<Long> allCandidates = new ArrayList<>();
		allCandidates.addAll(friendDiaryIds);
		allCandidates.addAll(publicDiaryIds);

		if (allCandidates.isEmpty()) {
			return Collections.emptyList();
		}

		List<RecommendationScoreResult> scoredDiaries = loadRecommendationForFeedPort.getRecommendedDiaries(
				userId, allCandidates, friendDiaryIds);

		int fixedSlotCount = (int) Math.ceil(requestedSize * FIXED_SLOT_RATIO);
		fixedSlotCount = Math.min(fixedSlotCount, friendDiaryIds.size());

		Set<Long> friendSet = new HashSet<>(friendDiaryIds);

		List<RecommendationScoreResult> friendScored = scoredDiaries.stream()
				.filter(sd -> friendSet.contains(sd.diaryId()))
				.limit(fixedSlotCount == 0 ? friendDiaryIds.size() : fixedSlotCount)
				.collect(Collectors.toList());

		Set<Long> usedIds = friendScored.stream()
				.map(RecommendationScoreResult::diaryId)
				.collect(Collectors.toSet());

		List<RecommendationScoreResult> remainingScored = scoredDiaries.stream()
				.filter(sd -> !usedIds.contains(sd.diaryId()))
				.collect(Collectors.toList());

		List<Long> result = new ArrayList<>();

		for (RecommendationScoreResult sd : friendScored) {
			result.add(sd.diaryId());
		}

		for (RecommendationScoreResult sd : remainingScored) {
			if (result.size() >= requestedSize)
				break;
			result.add(sd.diaryId());
		}

		log.debug("피드 구성 완료 - userId: {}, 고정슬롯: {}, 총: {}",
				userId, friendScored.size(), result.size());

		return result;
	}
}
