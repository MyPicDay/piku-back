package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.feed.application.port.out.*;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

import java.util.*;

/**
 * 피드 조회 서비스
 *
 * 책임:
 * - 피드 조회 API 처리
 * - 캐시 관리 (조회/저장)
 * - 클릭 로깅
 * - ResponseDTO 변환
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedQueryService implements GetFeedUseCase {

	private final LoadDiaryForFeedPort loadDiaryForFeedPort;
	private final LoadFeedListViewPort loadFeedListViewPort;
	private final LoadSocialForFeedPort loadSocialForFeedPort;
	private final LoadUserForFeedPort loadUserForFeedPort;
	private final LoadRecommendationForFeedPort loadRecommendationForFeedPort;
	private final LoadFeedClickPort loadFeedClickPort;
	private final SaveFeedClickPort saveFeedClickPort;

	private final FeedCandidateCollector feedCandidateCollector;
	private final FeedCompositionService feedCompositionService;

	@Override
	@Transactional(readOnly = true)
	public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId) {
		log.info("일기 상세 조회 요청 - diaryId: {}", diaryId);

		Diary diary = loadDiaryForFeedPort.getDiaryById(diaryId);
		List<String> photoUrls = loadDiaryForFeedPort.getPhotosForDiary(diary, requestMetaInfo);

		boolean isOwner = diary.getUserId().equals(userId);
		boolean isFriend = loadSocialForFeedPort.areFriends(diary.getUserId(), userId);
		boolean hasAccess = isOwner || (diary.getStatus() == DiaryVisibility.PUBLIC)
				|| (diary.getStatus() == DiaryVisibility.FRIENDS && isFriend);

		return buildResponseDTO(diary, photoUrls, requestMetaInfo, userId, null, hasAccess);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ResponseDTO> getAllDiaries(Pageable pageable, RequestMetaInfo requestMetaInfo, String userId) {
		List<Long> recommendedDiaryIds = getRecommendedDiaryIds(userId);
		List<Long> pagedDiaryIds = applyPaging(recommendedDiaryIds, pageable);
		List<FeedListItemView> feedItems = loadFeedListViewPort.loadFeedListItems(pagedDiaryIds, userId);
		List<ResponseDTO> responseList = feedItems.stream()
				.map(feedItem -> toResponseDTO(feedItem, requestMetaInfo))
				.toList();

		return new PageImpl<>(responseList, pageable, recommendedDiaryIds.size());
	}

	@Override
	@Transactional
	public void logClick(String userId, Long diaryId) {
		if (loadFeedClickPort.existsByUserIdAndDiaryId(userId, diaryId)) {
			return;
		}
		saveFeedClickPort.save(new FeedClick(userId, diaryId));
		updateUserPreferenceOnClick(userId, diaryId);

		loadRecommendationForFeedPort.invalidateCache(userId);
		log.debug("피드 캐시 무효화 - userId: {}", userId);
	}

	// ==================== Private Methods ====================

	private List<Long> getRecommendedDiaryIds(String userId) {
		List<Long> cachedIds = getCachedFeedIds(userId);
		if (!cachedIds.isEmpty()) {
			log.info("피드 캐시 히트 - userId: {}", userId);
			List<String> friendIds = userId != null ? loadSocialForFeedPort.getFriendIds(userId) : List.of();
			return loadDiaryForFeedPort.findRestorableFeedIds(cachedIds, userId, friendIds);
		}

		log.info("피드 캐시 미스 - userId: {}", userId);
		FeedCandidateCollector.FeedCandidates candidates = feedCandidateCollector.collect(userId);
		List<Long> scoredDiaryIds = applyScoring(candidates, userId);
		cacheFeed(userId, scoredDiaryIds);

		return scoredDiaryIds;
	}

	private List<Long> getCachedFeedIds(String userId) {
		if (userId == null) {
			return Collections.emptyList();
		}
		return loadRecommendationForFeedPort.getCachedFeed(userId);
	}

	private List<Long> applyScoring(FeedCandidateCollector.FeedCandidates candidates, String userId) {
		if (userId == null || candidates.orderedDiaryIds().isEmpty()) {
			return candidates.orderedDiaryIds();
		}

		return feedCompositionService.composeFeed(
				userId,
				candidates.friendDiaryIds(),
				candidates.publicDiaryIds(),
				candidates.orderedDiaryIds().size());
	}

	private void cacheFeed(String userId, List<Long> diaryIds) {
		if (userId == null || diaryIds.isEmpty()) {
			return;
		}
		loadRecommendationForFeedPort.cacheFeed(userId, diaryIds);
	}

	private void updateUserPreferenceOnClick(String userId, Long diaryId) {
		try {
			String topic = loadRecommendationForFeedPort.getMetadataTopic(diaryId)
					.orElse("daily");
			loadRecommendationForFeedPort.recordInteraction(userId, topic, "CLICK");
			log.debug("클릭 기반 선호도 업데이트 - userId: {}, topic: {}", userId, topic);
		} catch (Exception e) {
			log.warn("선호도 업데이트 실패 - userId: {}, diaryId: {}", userId, diaryId);
		}
	}

	private List<Long> applyPaging(List<Long> diaryIds, Pageable pageable) {
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), diaryIds.size());
		return start >= diaryIds.size() ? Collections.emptyList() : diaryIds.subList(start, end);
	}

	// ==================== DTO Builders ====================

	private ResponseDTO buildResponseDTO(Diary diary, List<String> photoUrls, RequestMetaInfo requestMetaInfo,
			String userId, FriendStatus friendStatus, boolean hasFullAccess) {
		String avatar = loadUserForFeedPort.getUserAvatar(diary.getUserId());
		String avatarUrl = loadUserForFeedPort.getUserAvatarUrl(avatar, requestMetaInfo);
		long likeCount = loadSocialForFeedPort.getLikeCount(diary.getId());
		boolean isLiked = loadSocialForFeedPort.isLikedByUser(userId, diary.getId());

		List<String> displayPhotos = hasFullAccess ? photoUrls : List.of(photoUrls.get(0));
		String displayContent = hasFullAccess ? diary.getContent() : null;

		return ResponseDTO.builder()
				.diaryId(diary.getId())
				.status(diary.getStatus())
				.content(displayContent)
				.imgUrls(displayPhotos)
				.date(diary.getDate())
				.nickname(loadUserForFeedPort.getUserNickname(diary.getUserId()))
				.avatar(avatarUrl)
				.userId(diary.getUserId())
				.createdAt(diary.getCreatedAt())
				.friendStatus(friendStatus)
				.commentCount(loadSocialForFeedPort.countComments(diary.getId()))
				.likeCount(likeCount)
				.isLiked(isLiked)
				.build();
	}

	private ResponseDTO toResponseDTO(FeedListItemView feedItem, RequestMetaInfo requestMetaInfo) {
		String avatarUrl = feedItem.avatarPath() != null
				? loadUserForFeedPort.getUserAvatarUrl(feedItem.avatarPath(), requestMetaInfo)
				: null;

		return ResponseDTO.builder()
				.diaryId(feedItem.diaryId())
				.status(feedItem.status())
				.content(feedItem.content())
				.imgUrls(feedItem.imageUrls())
				.date(feedItem.date())
				.nickname(feedItem.nickname())
				.avatar(avatarUrl)
				.userId(feedItem.userId())
				.createdAt(feedItem.createdAt())
				.friendStatus(feedItem.friendStatus())
				.commentCount(feedItem.commentCount())
				.likeCount(feedItem.likeCount())
				.isLiked(feedItem.liked())
				.build();
	}
}
