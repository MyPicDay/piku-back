package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedFriendStatus;
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.feed.application.port.out.*;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.*;

/**
 * 피드 조회 서비스
 *
 * 책임:
 * - 피드 상세 조회 처리
 * - cursor 기반 피드 목록 조회 orchestration
 * - 클릭 로깅 및 선호도 이벤트 반영
 * - ResponseDTO 변환
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedQueryService implements GetFeedUseCase {

	private final LoadDiaryForFeedPort loadDiaryForFeedPort;
	private final LoadFeedListViewPort loadFeedListViewPort;
	private final LoadFeedCursorCandidatesPort loadFeedCursorCandidatesPort;
	private final LoadSocialForFeedPort loadSocialForFeedPort;
	private final LoadUserForFeedPort loadUserForFeedPort;
	private final LoadRecommendationForFeedPort loadRecommendationForFeedPort;
	private final LoadFeedClickPort loadFeedClickPort;
	private final SaveFeedClickPort saveFeedClickPort;
	private final FeedCursorTokenCodec feedCursorTokenCodec;

	@Override
	@Transactional(readOnly = true)
	public FeedDiaryResult getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId) {
		log.info("일기 상세 조회 요청 - diaryId: {}", diaryId);

		FeedDiaryDetailView diary = loadDiaryForFeedPort.findVisibleDiaryById(diaryId, userId)
				.orElseThrow(FeedDiaryNotFoundException::new);
		return buildResponseDTO(diary, requestMetaInfo, userId, null);
	}

	@Override
	@Transactional(readOnly = true)
	public FeedCursorPage<FeedDiaryResult> getAllDiaries(FeedCursorRequest request, RequestMetaInfo requestMetaInfo, String userId) {
		FeedCursor cursor = decodeCursor(request.cursor(), userId);
		List<FeedCursorCandidate> candidates = loadCursorPageCandidates(userId, cursor, request.limit());
		List<Long> diaryIds = candidates.stream()
				.map(FeedCursorCandidate::diaryId)
				.toList();
		List<FeedListItemView> feedItems = loadFeedListViewPort.loadFeedListItems(diaryIds, userId);
		List<FeedDiaryResult> responseList = feedItems.stream()
				.map(feedItem -> toResponseDTO(feedItem, requestMetaInfo))
				.toList();
		boolean hasNext = hasNext(userId, candidates, request.limit());
		String nextCursor = hasNext && !candidates.isEmpty()
				? feedCursorTokenCodec.encode(candidates.get(candidates.size() - 1).toCursor())
				: null;

		return new FeedCursorPage<>(responseList, nextCursor, hasNext);
	}

	@Override
	@Transactional
	public void logClick(String userId, Long diaryId) {
		Optional<FeedDiaryDetailView> visibleDiary = loadDiaryForFeedPort.findVisibleDiaryById(diaryId, userId);
		if (visibleDiary.isEmpty()) {
			return;
		}
		if (loadFeedClickPort.existsByUserIdAndDiaryId(userId, diaryId)) {
			return;
		}
		saveFeedClickPort.save(new FeedClick(userId, diaryId));
		updateUserPreferenceOnClick(userId, diaryId);
	}

	// ==================== Private Methods ====================

	private FeedCursor decodeCursor(String cursorToken, String userId) {
		FeedCursor cursor = feedCursorTokenCodec.decode(cursorToken);
		if (cursor == null) {
			return null;
		}
		if (!FeedBucket.orderedBuckets(userId).contains(cursor.bucket())) {
			throw new InvalidFeedCursorException();
		}
		return cursor;
	}

	private List<FeedCursorCandidate> loadCursorPageCandidates(String userId, FeedCursor cursor, int limit) {
		List<FeedCursorCandidate> collected = new ArrayList<>();
		FeedBucket bucket = cursor != null ? cursor.bucket() : FeedBucket.firstBucket(userId);
		FeedCursor bucketCursor = cursor;
		int remaining = limit;

		while (bucket != null && remaining > 0) {
			List<FeedCursorCandidate> candidates = loadFeedCursorCandidatesPort.loadCandidates(
					userId,
					bucket,
					bucketCursor,
					remaining);
			collected.addAll(candidates);
			remaining -= candidates.size();
			bucket = bucket.next(userId);
			bucketCursor = null;
		}

		return collected;
	}

	private boolean hasNext(String userId, List<FeedCursorCandidate> candidates, int limit) {
		if (candidates.size() < limit || candidates.isEmpty()) {
			return false;
		}

		FeedCursorCandidate lastCandidate = candidates.get(candidates.size() - 1);
		List<FeedCursorCandidate> sameBucketRemainder = loadFeedCursorCandidatesPort.loadCandidates(
				userId,
				lastCandidate.bucket(),
				lastCandidate.toCursor(),
				1);
		if (!sameBucketRemainder.isEmpty()) {
			return true;
		}

		FeedBucket nextBucket = lastCandidate.bucket().next(userId);
		while (nextBucket != null) {
			List<FeedCursorCandidate> nextBucketItems = loadFeedCursorCandidatesPort.loadCandidates(
					userId,
					nextBucket,
					null,
					1);
			if (!nextBucketItems.isEmpty()) {
				return true;
			}
			nextBucket = nextBucket.next(userId);
		}

		return false;
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

	// ==================== DTO Builders ====================

	private FeedDiaryResult buildResponseDTO(FeedDiaryDetailView diary, RequestMetaInfo requestMetaInfo,
			String userId, FeedFriendStatus friendStatus) {
		String avatar = loadUserForFeedPort.getUserAvatar(diary.userId());
		String avatarUrl = loadUserForFeedPort.getUserAvatarUrl(avatar, requestMetaInfo);
		long likeCount = loadSocialForFeedPort.getLikeCount(diary.diaryId());
		boolean isLiked = loadSocialForFeedPort.isLikedByUser(userId, diary.diaryId());

		return FeedDiaryResult.builder()
				.diaryId(diary.diaryId())
				.status(diary.status())
				.content(diary.content())
				.imgUrls(diary.imageUrls())
				.date(diary.date())
				.nickname(loadUserForFeedPort.getUserNickname(diary.userId()))
				.avatar(avatarUrl)
				.userId(diary.userId())
				.createdAt(diary.createdAt())
				.friendStatus(friendStatus)
				.commentCount(loadSocialForFeedPort.countComments(userId, diary.diaryId()))
				.likeCount(likeCount)
				.isLiked(isLiked)
				.build();
	}

	private FeedDiaryResult toResponseDTO(FeedListItemView feedItem, RequestMetaInfo requestMetaInfo) {
		String avatarUrl = feedItem.avatarPath() != null
				? loadUserForFeedPort.getUserAvatarUrl(feedItem.avatarPath(), requestMetaInfo)
				: null;

		return FeedDiaryResult.builder()
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
