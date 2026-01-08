package store.piku.back.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.FeedClickRepository;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 피드 후보 수집을 담당하는 서비스
 * 책임: 친구/공개 피드 조회, 읽음 상태 기반 우선순위 정렬, 본인 일기 제외
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCandidateCollector {

	private final DiaryRepository diaryRepository;
	private final FeedClickRepository feedClickRepository;
	private final FriendRequestService friendRequestService;

	/**
	 * 피드 후보를 우선순위에 따라 수집
	 * 
	 * 우선순위:
	 * 1. 미읽음 친구 피드
	 * 2. 미읽음 공개 피드
	 * 3. 읽은 친구 피드
	 * 4. 읽은 공개 피드
	 */
	public List<Diary> collect(String userId, Pageable pageable, RequestMetaInfo requestMetaInfo) {
		Set<Long> clickedFeedIds = getClickedFeedIds(userId);
		List<Diary> friendFeeds = getFriendFeeds(userId, pageable, requestMetaInfo);
		List<Diary> publicFeeds = getPublicFeeds();

		return combineFeedsByPriority(userId, friendFeeds, publicFeeds, clickedFeedIds);
	}

	private Set<Long> getClickedFeedIds(String userId) {
		if (userId == null) {
			return Collections.emptySet();
		}
		return new HashSet<>(feedClickRepository.findClickedDiaryIdsByUserId(userId));
	}

	private List<Diary> getFriendFeeds(String userId, Pageable pageable, RequestMetaInfo requestMetaInfo) {
		if (userId == null) {
			return Collections.emptyList();
		}
		List<String> friendIds = friendRequestService.findFriendIdList(pageable, userId, requestMetaInfo);
		return diaryRepository.findByStatusAndUserIdIn(Status.FRIENDS, friendIds);
	}

	private List<Diary> getPublicFeeds() {
		return diaryRepository.findByStatusOrderByCreatedAtDesc(Status.PUBLIC);
	}

	private List<Diary> combineFeedsByPriority(String userId, List<Diary> friendFeeds,
			List<Diary> publicFeeds, Set<Long> clickedFeedIds) {
		Set<Long> addedIds = new HashSet<>();
		List<Diary> combined = new ArrayList<>();

		// 1순위: 미읽음 친구 피드
		addFilteredFeeds(combined, addedIds, friendFeeds, userId, clickedFeedIds, false);

		// 2순위: 미읽음 공개 피드
		addFilteredFeeds(combined, addedIds, publicFeeds, userId, clickedFeedIds, false);

		// 3순위: 읽은 친구 피드
		addFilteredFeeds(combined, addedIds, friendFeeds, userId, clickedFeedIds, true);

		// 4순위: 읽은 공개 피드
		addFilteredFeeds(combined, addedIds, publicFeeds, userId, clickedFeedIds, true);

		log.debug("피드 후보 수집 완료 - 총: {}", combined.size());
		return combined;
	}

	/**
	 * 피드를 필터링하여 추가하는 헬퍼 메서드
	 * 중복 코드 제거를 위해 추출됨
	 */
	private void addFilteredFeeds(List<Diary> target, Set<Long> addedIds, List<Diary> source,
			String userId, Set<Long> clickedFeedIds, boolean includeClicked) {
		source.stream()
				.filter(d -> !isOwnDiary(d, userId))
				.filter(d -> includeClicked == clickedFeedIds.contains(d.getId()))
				.forEach(d -> {
					if (addedIds.add(d.getId())) {
						target.add(d);
					}
				});
	}

	private boolean isOwnDiary(Diary diary, String userId) {
		return userId != null && diary.getUser().getId().equals(userId);
	}
}
