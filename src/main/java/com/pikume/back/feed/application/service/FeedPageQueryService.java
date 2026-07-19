package com.pikume.back.feed.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedLatestCursorCandidate;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.exception.InvalidFeedCursorException;
import com.pikume.back.feed.application.port.in.QueryFeedPageUseCase;
import com.pikume.back.feed.application.port.out.LoadFeedFriendshipPort;
import com.pikume.back.feed.application.port.out.LoadLatestFeedCandidatesPort;
import com.pikume.back.feed.application.readmodel.FeedListItemView;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedPageQueryService implements QueryFeedPageUseCase {

	private final FeedListItemAssembler feedListItemAssembler;
	private final RecommendedFeedCandidateService recommendedFeedCandidateService;
	private final LoadLatestFeedCandidatesPort loadLatestFeedCandidatesPort;
	private final LoadFeedFriendshipPort loadFeedFriendshipPort;
	private final FeedCursorTokenCodec feedCursorTokenCodec;

	@Override
	@Transactional(readOnly = true)
	public FeedCursorPage<FeedDiaryResult> queryPage(FeedCursorRequest request, String userId) {
		FeedCursor cursor = decodeCursor(request.cursor(), userId, request.sortMode());
		if (request.sortMode() == FeedSortMode.LATEST) {
			return getLatestDiaries(request, userId, cursor);
		}
		return getRecommendedDiaries(request, userId, cursor);
	}

	private FeedCursorPage<FeedDiaryResult> getRecommendedDiaries(FeedCursorRequest request, String userId,
			FeedCursor cursor) {
		List<FeedCursorCandidate> candidates = loadRecommendedPageCandidates(userId, cursor, request.limit());
		List<Long> diaryIds = candidates.stream()
				.map(FeedCursorCandidate::diaryId)
				.toList();
		List<FeedListItemView> feedItems = feedListItemAssembler.assemble(diaryIds, userId);
		List<FeedDiaryResult> responseList = feedItems.stream()
				.map(this::toResult)
				.toList();
		boolean hasNext = hasNextRecommended(userId, candidates, request.limit());
		String nextCursor = hasNext && !candidates.isEmpty()
				? feedCursorTokenCodec.encode(candidates.get(candidates.size() - 1).toCursor())
				: null;

		return new FeedCursorPage<>(responseList, nextCursor, hasNext);
	}

	private FeedCursorPage<FeedDiaryResult> getLatestDiaries(FeedCursorRequest request, String userId,
			FeedCursor cursor) {
		List<String> friendUserIds = resolveFriendUserIds(userId);
		List<FeedLatestCursorCandidate> candidates = loadLatestPageCandidates(userId, friendUserIds, cursor, request.limit());
		List<Long> diaryIds = candidates.stream()
				.map(FeedLatestCursorCandidate::diaryId)
				.toList();
		List<FeedListItemView> feedItems = feedListItemAssembler.assemble(diaryIds, userId);
		List<FeedDiaryResult> responseList = feedItems.stream()
				.map(this::toResult)
				.toList();
		boolean hasNext = hasNextLatest(userId, friendUserIds, candidates, request.limit());
		String nextCursor = hasNext && !candidates.isEmpty()
				? feedCursorTokenCodec.encode(candidates.get(candidates.size() - 1).toCursor())
				: null;

		return new FeedCursorPage<>(responseList, nextCursor, hasNext);
	}

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
		if (cursor.date() == null || cursor.diaryId() <= 0) {
			throw new InvalidFeedCursorException();
		}
	}

	private List<FeedCursorCandidate> loadRecommendedPageCandidates(String userId, FeedCursor cursor, int limit) {
		List<FeedCursorCandidate> collected = new ArrayList<>();
		FeedBucket bucket = cursor != null ? cursor.bucket() : FeedBucket.firstBucket(userId);
		FeedCursor bucketCursor = cursor;
		int remaining = limit;

		while (bucket != null && remaining > 0) {
			List<FeedCursorCandidate> candidates = recommendedFeedCandidateService.loadCandidates(
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
		return loadFeedFriendshipPort.loadFriendUserIds(userId);
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
		List<FeedCursorCandidate> sameBucketRemainder = recommendedFeedCandidateService.loadCandidates(
				userId,
				lastCandidate.bucket(),
				lastCandidate.toCursor(),
				1);
		if (!sameBucketRemainder.isEmpty()) {
			return true;
		}

		FeedBucket nextBucket = lastCandidate.bucket().next(userId);
		while (nextBucket != null) {
			List<FeedCursorCandidate> nextBucketItems = recommendedFeedCandidateService.loadCandidates(
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

	private FeedDiaryResult toResult(FeedListItemView feedItem) {
		return FeedDiaryResult.builder()
				.diaryId(feedItem.diaryId())
				.status(feedItem.status())
				.content(feedItem.content())
				.imgUrls(feedItem.imageUrls())
				.date(feedItem.date())
				.nickname(feedItem.nickname())
				.avatar(feedItem.avatarUrl())
				.userId(feedItem.userId())
				.createdAt(feedItem.createdAt())
				.friendStatus(feedItem.friendStatus())
				.commentCount(feedItem.commentCount())
				.likeCount(feedItem.likeCount())
				.isLiked(feedItem.liked())
				.isOwner(feedItem.owner())
				.build();
	}
}
