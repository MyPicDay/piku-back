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
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.feed.application.port.out.*;
import com.pikume.back.feed.application.readmodel.FeedDiaryDetailView;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.global.dto.RequestMetaInfo;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
	private final LoadLatestFeedCandidatesPort loadLatestFeedCandidatesPort;
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
		long requestStartedAt = System.nanoTime();
		long decodeStartedAt = System.nanoTime();
		FeedCursor cursor = decodeCursor(request.cursor(), userId, request.sortMode());
		if (request.sortMode() == FeedSortMode.LATEST) {
			log.info("event=feed_latest_stage outcome=success stage=decode_cursor userId={} limit={} cursorPresent={} durationMs={}",
					userId,
					request.limit(),
					hasCursor(request.cursor()),
					elapsedMillis(decodeStartedAt));
			return getLatestDiaries(request, requestMetaInfo, userId, cursor, requestStartedAt);
		}
		return getRecommendedDiaries(request, requestMetaInfo, userId, cursor);
	}

	private FeedCursorPage<FeedDiaryResult> getRecommendedDiaries(FeedCursorRequest request,
			RequestMetaInfo requestMetaInfo, String userId, FeedCursor cursor) {
		List<FeedCursorCandidate> candidates = loadRecommendedPageCandidates(userId, cursor, request.limit());
		List<Long> diaryIds = candidates.stream()
				.map(FeedCursorCandidate::diaryId)
				.toList();
		List<FeedListItemView> feedItems = loadFeedListViewPort.loadFeedListItems(diaryIds, userId);
		List<FeedDiaryResult> responseList = feedItems.stream()
				.map(feedItem -> toResponseDTO(feedItem, requestMetaInfo))
				.toList();
		boolean hasNext = hasNextRecommended(userId, candidates, request.limit());
		String nextCursor = hasNext && !candidates.isEmpty()
				? feedCursorTokenCodec.encode(candidates.get(candidates.size() - 1).toCursor())
				: null;

		return new FeedCursorPage<>(responseList, nextCursor, hasNext);
	}

	private FeedCursorPage<FeedDiaryResult> getLatestDiaries(FeedCursorRequest request,
			RequestMetaInfo requestMetaInfo, String userId, FeedCursor cursor, long requestStartedAt) {
		log.info("event=feed_latest_started outcome=started userId={} limit={} cursorPresent={}",
				userId,
				request.limit(),
				cursor != null);

		long stageStartedAt = System.nanoTime();
		List<String> friendUserIds = resolveFriendUserIds(userId);
		log.info("event=feed_latest_stage outcome=success stage=resolve_friends userId={} friendCount={} durationMs={}",
				userId,
				friendUserIds.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		List<FeedLatestCursorCandidate> candidates = loadLatestPageCandidates(userId, friendUserIds, cursor, request.limit());
		log.info("event=feed_latest_stage outcome=success stage=load_candidates userId={} limit={} cursorPresent={} friendCount={} candidateCount={} durationMs={}",
				userId,
				request.limit(),
				cursor != null,
				friendUserIds.size(),
				candidates.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		List<Long> diaryIds = candidates.stream()
				.map(FeedLatestCursorCandidate::diaryId)
				.toList();
		log.info("event=feed_latest_stage outcome=success stage=extract_candidate_ids userId={} candidateCount={} durationMs={}",
				userId,
				candidates.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		List<FeedListItemView> feedItems = loadFeedListViewPort.loadFeedListItems(diaryIds, userId);
		log.info("event=feed_latest_stage outcome=success stage=materialize_feed_items userId={} candidateCount={} itemCount={} durationMs={}",
				userId,
				diaryIds.size(),
				feedItems.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		List<FeedDiaryResult> responseList = feedItems.stream()
				.map(feedItem -> toResponseDTO(feedItem, requestMetaInfo))
				.toList();
		log.info("event=feed_latest_stage outcome=success stage=build_response_dtos userId={} itemCount={} durationMs={}",
				userId,
				responseList.size(),
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		boolean hasNext = hasNextLatest(userId, friendUserIds, candidates, request.limit());
		log.info("event=feed_latest_stage outcome=success stage=resolve_has_next userId={} candidateCount={} limit={} hasNext={} durationMs={}",
				userId,
				candidates.size(),
				request.limit(),
				hasNext,
				elapsedMillis(stageStartedAt));

		stageStartedAt = System.nanoTime();
		String nextCursor = hasNext && !candidates.isEmpty()
				? feedCursorTokenCodec.encode(candidates.get(candidates.size() - 1).toCursor())
				: null;
		log.info("event=feed_latest_stage outcome=success stage=encode_next_cursor userId={} nextCursorPresent={} durationMs={}",
				userId,
				nextCursor != null,
				elapsedMillis(stageStartedAt));
		log.info("event=feed_latest_completed outcome=success userId={} limit={} candidateCount={} itemCount={} hasNext={} totalDurationMs={}",
				userId,
				request.limit(),
				candidates.size(),
				responseList.size(),
				hasNext,
				elapsedMillis(requestStartedAt));

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

	private FeedCursor decodeCursor(String cursorToken, String userId, FeedSortMode requestedSortMode) {
		FeedCursor cursor = feedCursorTokenCodec.decode(cursorToken);
		if (cursor == null) {
			return null;
		}
		validateCursorSortMode(cursor, requestedSortMode);
		if (requestedSortMode == FeedSortMode.LATEST) {
			validateLatestCursor(cursor);
			return cursor;
		}
		if (!FeedBucket.orderedBuckets(userId).contains(cursor.bucket())) {
			throw new InvalidFeedCursorException();
		}
		if (cursor.createdAt() == null || cursor.diaryId() <= 0) {
			throw new InvalidFeedCursorException();
		}
		return cursor;
	}

	private void validateCursorSortMode(FeedCursor cursor, FeedSortMode requestedSortMode) {
		FeedSortMode cursorSortMode = cursor.sortMode();
		if (cursorSortMode == null) {
			if (requestedSortMode != FeedSortMode.RECOMMENDED) {
				throw new InvalidFeedCursorException();
			}
			return;
		}
		if (cursorSortMode != requestedSortMode) {
			throw new InvalidFeedCursorException();
		}
	}

	private void validateLatestCursor(FeedCursor cursor) {
		if (cursor.createdAt() == null || cursor.diaryId() <= 0) {
			throw new InvalidFeedCursorException();
		}
	}

	private List<FeedCursorCandidate> loadRecommendedPageCandidates(String userId, FeedCursor cursor, int limit) {
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

	private List<String> resolveFriendUserIds(String userId) {
		if (!FeedBucket.hasUser(userId)) {
			return List.of();
		}
		return loadSocialForFeedPort.getFriendIds(userId);
	}

	private List<FeedLatestCursorCandidate> loadLatestPageCandidates(String userId, List<String> friendUserIds,
			FeedCursor cursor, int limit) {
		return loadLatestFeedCandidatesPort.loadCandidates(userId, friendUserIds, cursor, limit);
	}

	private boolean hasNextRecommended(String userId, List<FeedCursorCandidate> candidates, int limit) {
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

	private boolean hasNextLatest(String userId, List<String> friendUserIds, List<FeedLatestCursorCandidate> candidates,
			int limit) {
		if (candidates.size() < limit || candidates.isEmpty()) {
			return false;
		}

		FeedLatestCursorCandidate lastCandidate = candidates.get(candidates.size() - 1);
		return !loadLatestFeedCandidatesPort.loadCandidates(
				userId,
				friendUserIds,
				lastCandidate.toCursor(),
				1).isEmpty();
	}

	private boolean hasCursor(String cursorToken) {
		return cursorToken != null && !cursorToken.isBlank();
	}

	private long elapsedMillis(long startedAtNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
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
