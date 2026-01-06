package store.piku.back.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.piku.back.recommendation.dto.ScoredDiary;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCompositionService {

	private final RecommendationService recommendationService;

	private static final double FIXED_SLOT_RATIO = 0.3;

	public List<Long> composeFeed(String userId, List<Long> friendDiaryIds,
			List<Long> publicDiaryIds, int requestedSize) {
		List<Long> allCandidates = new ArrayList<>();
		allCandidates.addAll(friendDiaryIds);
		allCandidates.addAll(publicDiaryIds);

		if (allCandidates.isEmpty()) {
			return Collections.emptyList();
		}

		List<ScoredDiary> scoredDiaries = recommendationService.getRecommendedDiaries(
				userId, allCandidates, friendDiaryIds);

		int fixedSlotCount = (int) Math.ceil(friendDiaryIds.size() * FIXED_SLOT_RATIO);
		fixedSlotCount = Math.min(fixedSlotCount, friendDiaryIds.size());

		Set<Long> friendSet = new HashSet<>(friendDiaryIds);

		List<ScoredDiary> friendScored = scoredDiaries.stream()
				.filter(sd -> friendSet.contains(sd.getDiaryId()))
				.limit(fixedSlotCount == 0 ? friendDiaryIds.size() : fixedSlotCount)
				.collect(Collectors.toList());

		Set<Long> usedIds = friendScored.stream()
				.map(ScoredDiary::getDiaryId)
				.collect(Collectors.toSet());

		List<ScoredDiary> remainingScored = scoredDiaries.stream()
				.filter(sd -> !usedIds.contains(sd.getDiaryId()))
				.collect(Collectors.toList());

		List<Long> result = new ArrayList<>();

		for (ScoredDiary sd : friendScored) {
			result.add(sd.getDiaryId());
		}

		for (ScoredDiary sd : remainingScored) {
			if (result.size() >= requestedSize)
				break;
			result.add(sd.getDiaryId());
		}

		log.debug("피드 구성 완료 - userId: {}, 고정슬롯: {}, 총: {}",
				userId, friendScored.size(), result.size());

		return result;
	}
}
