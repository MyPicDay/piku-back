package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.feed.application.port.out.*;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.feed.domain.exception.InvalidFeedCursorException;
import com.pikume.back.social.domain.friend.vo.FriendStatus;

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
	public FeedCursorPage<ResponseDTO> getAllDiaries(FeedCursorRequest request, RequestMetaInfo requestMetaInfo, String userId) {
		FeedCursor cursor = decodeCursor(request.cursor(), userId);
		List<FeedCursorCandidate> candidates = loadCursorPageCandidates(userId, cursor, request.limit());
		List<Long> diaryIds = candidates.stream()
				.map(FeedCursorCandidate::diaryId)
				.toList();
		List<FeedListItemView> feedItems = loadFeedListViewPort.loadFeedListItems(diaryIds, userId);
		List<ResponseDTO> responseList = feedItems.stream()
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
