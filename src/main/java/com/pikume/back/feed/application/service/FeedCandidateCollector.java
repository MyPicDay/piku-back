package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;

import java.util.*;

/**
 * 피드 후보 수집을 담당하는 서비스
 * 책임: 친구/공개 피드 조회, 읽음 상태 기반 우선순위 정렬, 본인 일기 제외
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCandidateCollector {

	private static final int SOURCE_QUERY_MULTIPLIER = 2;
	private static final int MAX_SOURCE_QUERY_LIMIT = 400;

	private final LoadDiaryForFeedPort loadDiaryForFeedPort;
	private final LoadFeedClickPort loadFeedClickPort;
	private final LoadSocialForFeedPort loadSocialForFeedPort;

	/**
	 * 피드 후보를 우선순위에 따라 수집
	 *
	 * 우선순위:
	 * 1. 미읽음 친구 피드
	 * 2. 미읽음 공개 피드
	 * 3. 읽은 친구 피드
	 * 4. 읽은 공개 피드
	 */
	public FeedCandidates collect(String userId, int candidateLimit) {
		Set<Long> clickedFeedIds = getClickedFeedIds(userId);
		List<String> friendIds = getFriendIds(userId);
		int sourceQueryLimit = getSourceQueryLimit(candidateLimit);
		List<Long> friendFeedIds = getFriendFeedIds(friendIds, sourceQueryLimit);
		List<Long> friendPublicFeedIds = getFriendPublicFeedIds(friendIds, sourceQueryLimit);
		List<Long> publicFeedIds = getPublicFeedIds(userId, sourceQueryLimit);
		List<Long> orderedDiaryIds = combineFeedIdsByPriority(
				friendFeedIds,
				publicFeedIds,
				clickedFeedIds,
				candidateLimit);
		List<Long> friendAuthoredDiaryIds = collectFriendAuthoredDiaryIdsInOrder(
				orderedDiaryIds, friendFeedIds, friendPublicFeedIds);
		List<Long> nonFriendPublicDiaryIds = collectNonFriendDiaryIdsInOrder(orderedDiaryIds, friendAuthoredDiaryIds);

		return new FeedCandidates(orderedDiaryIds, friendAuthoredDiaryIds, nonFriendPublicDiaryIds);
	}

	private Set<Long> getClickedFeedIds(String userId) {
		if (userId == null) {
			return Collections.emptySet();
		}
		return new HashSet<>(loadFeedClickPort.findClickedDiaryIdsByUserId(userId));
	}

	private List<String> getFriendIds(String userId) {
		if (userId == null) {
			return List.of();
		}

		return loadSocialForFeedPort.getFriendIds(userId);
	}

	private int getSourceQueryLimit(int candidateLimit) {
		return Math.min(candidateLimit * SOURCE_QUERY_MULTIPLIER, MAX_SOURCE_QUERY_LIMIT);
	}

	private List<Long> getFriendFeedIds(List<String> friendIds, int limit) {
		if (friendIds.isEmpty()) {
			return List.of();
		}

		return loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(DiaryVisibility.FRIENDS, friendIds, limit);
	}

	private List<Long> getFriendPublicFeedIds(List<String> friendIds, int limit) {
		if (friendIds.isEmpty()) {
			return List.of();
		}

		return loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(DiaryVisibility.PUBLIC, friendIds, limit);
	}

	private List<Long> getPublicFeedIds(String userId, int limit) {
		return loadDiaryForFeedPort.findFeedIdsByStatus(DiaryVisibility.PUBLIC, userId, limit);
	}

	private List<Long> combineFeedIdsByPriority(List<Long> friendFeedIds, List<Long> publicFeedIds,
			Set<Long> clickedFeedIds,
			int candidateLimit) {
		Set<Long> addedIds = new HashSet<>();
		List<Long> combined = new ArrayList<>();

		// 1순위: 미읽음 친구 피드
		addFilteredFeedIds(combined, addedIds, friendFeedIds, clickedFeedIds, false, candidateLimit);

		// 2순위: 미읽음 공개 피드
		addFilteredFeedIds(combined, addedIds, publicFeedIds, clickedFeedIds, false, candidateLimit);

		// 3순위: 읽은 친구 피드
		addFilteredFeedIds(combined, addedIds, friendFeedIds, clickedFeedIds, true, candidateLimit);

		// 4순위: 읽은 공개 피드
		addFilteredFeedIds(combined, addedIds, publicFeedIds, clickedFeedIds, true, candidateLimit);

		log.debug("피드 후보 수집 완료 - 총: {}", combined.size());
		return combined;
	}

	private List<Long> collectFriendAuthoredDiaryIdsInOrder(List<Long> orderedDiaryIds, List<Long> friendFeedIds,
			List<Long> friendPublicFeedIds) {
		Set<Long> friendDiaryIdSet = new HashSet<>(friendFeedIds);
		friendDiaryIdSet.addAll(friendPublicFeedIds);

		return orderedDiaryIds.stream()
				.filter(friendDiaryIdSet::contains)
				.toList();
	}

	private List<Long> collectNonFriendDiaryIdsInOrder(List<Long> orderedDiaryIds, List<Long> friendAuthoredDiaryIds) {
		if (friendAuthoredDiaryIds.isEmpty()) {
			return orderedDiaryIds;
		}

		Set<Long> friendDiaryIdSet = new HashSet<>(friendAuthoredDiaryIds);
		return orderedDiaryIds.stream()
				.filter(diaryId -> !friendDiaryIdSet.contains(diaryId))
				.toList();
	}

	private void addFilteredFeedIds(List<Long> target, Set<Long> addedIds, List<Long> source,
			Set<Long> clickedFeedIds, boolean includeClicked, int candidateLimit) {
		for (Long diaryId : source) {
			if (target.size() >= candidateLimit) {
				return;
			}
			if (includeClicked != clickedFeedIds.contains(diaryId)) {
				continue;
			}
			if (addedIds.add(diaryId)) {
				target.add(diaryId);
			}
		}
	}

	public record FeedCandidates(
			List<Long> orderedDiaryIds,
			List<Long> friendDiaryIds,
			List<Long> publicDiaryIds
	) {
	}
}
