package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.port.out.LoadDiaryForFeedPort;
import com.pikume.back.feed.application.port.out.LoadFeedClickPort;
import com.pikume.back.feed.application.port.out.LoadFeedCursorCandidatesPort;
import com.pikume.back.feed.application.port.out.LoadSocialForFeedPort;
import com.pikume.back.feed.application.readmodel.FeedDiaryCandidateView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedCursorPersistenceAdapter implements LoadFeedCursorCandidatesPort {

	private static final int SOURCE_QUERY_MULTIPLIER = 4;
	private static final int MIN_SOURCE_QUERY_LIMIT = 50;
	private static final int MAX_SOURCE_QUERY_LIMIT = 800;

	private static final Comparator<FeedCursorCandidate> FEED_CURSOR_ORDER =
			Comparator.comparing(FeedCursorCandidate::createdAt).reversed()
					.thenComparing(FeedCursorCandidate::likeCount, Comparator.reverseOrder())
					.thenComparing(FeedCursorCandidate::commentCount, Comparator.reverseOrder())
					.thenComparing(FeedCursorCandidate::diaryId, Comparator.reverseOrder());

	private final LoadDiaryForFeedPort loadDiaryForFeedPort;
	private final LoadSocialForFeedPort loadSocialForFeedPort;
	private final LoadFeedClickPort loadFeedClickPort;

	@Override
	public List<FeedCursorCandidate> loadCandidates(String currentUserId, FeedBucket bucket, FeedCursor cursor, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		if (!FeedBucket.hasUser(currentUserId) && bucket != FeedBucket.NOT_CONSUMED_PUBLIC) {
			return List.of();
		}

		int sourceQueryLimit = initialSourceQueryLimit(limit);
		List<FeedCursorCandidate> candidates = List.of();

		while (sourceQueryLimit <= MAX_SOURCE_QUERY_LIMIT) {
			candidates = collectCandidates(currentUserId, bucket, cursor, sourceQueryLimit, limit);
			if (candidates.size() >= limit || sourceQueryLimit == MAX_SOURCE_QUERY_LIMIT) {
				return candidates;
			}
			sourceQueryLimit = Math.min(sourceQueryLimit * 2, MAX_SOURCE_QUERY_LIMIT);
		}

		return candidates;
	}

	private List<FeedCursorCandidate> collectCandidates(String currentUserId, FeedBucket bucket, FeedCursor cursor,
			int sourceQueryLimit, int limit) {
		Set<String> friendUserIds = resolveFriendUserIds(currentUserId);
		List<Long> sourceDiaryIds = bucket.isFriendBucket()
				? loadFriendBucketDiaryIds(friendUserIds, sourceQueryLimit)
				: loadPublicBucketDiaryIds(currentUserId, friendUserIds, sourceQueryLimit);

		if (sourceDiaryIds.isEmpty()) {
			return List.of();
		}

		Map<Long, FeedDiaryCandidateView> diaryCandidatesById = loadDiaryForFeedPort.getFeedDiaryCandidates(Set.copyOf(sourceDiaryIds));
		List<Long> availableDiaryIds = sourceDiaryIds.stream()
				.filter(diaryCandidatesById::containsKey)
				.toList();
		if (availableDiaryIds.isEmpty()) {
			return List.of();
		}

		Set<Long> consumedDiaryIds = resolveConsumedDiaryIds(currentUserId, availableDiaryIds);
		Map<Long, Long> likeCountsByDiaryId = loadSocialForFeedPort.getLikeCountsForDiaries(availableDiaryIds);
		Map<Long, Long> commentCountsByDiaryId = loadSocialForFeedPort.getCommentCountsForDiaries(availableDiaryIds);

		Predicate<FeedCursorCandidate> bucketFilter = bucket.isConsumedBucket()
				? candidate -> consumedDiaryIds.contains(candidate.diaryId())
				: candidate -> !consumedDiaryIds.contains(candidate.diaryId());

		return availableDiaryIds.stream()
				.map(diaryId -> toCandidate(
						bucket,
						diaryCandidatesById.get(diaryId),
						likeCountsByDiaryId.getOrDefault(diaryId, 0L),
						commentCountsByDiaryId.getOrDefault(diaryId, 0L)))
				.filter(bucketFilter)
				.filter(candidate -> isAfterCursor(candidate, cursor))
				.sorted(FEED_CURSOR_ORDER)
				.limit(limit)
				.toList();
	}

	private Set<String> resolveFriendUserIds(String currentUserId) {
		if (!FeedBucket.hasUser(currentUserId)) {
			return Set.of();
		}
		return new HashSet<>(loadSocialForFeedPort.getFriendIds(currentUserId));
	}

	private List<Long> loadFriendBucketDiaryIds(Set<String> friendUserIds, int sourceQueryLimit) {
		if (friendUserIds.isEmpty()) {
			return List.of();
		}

		LinkedHashSet<Long> combined = new LinkedHashSet<>();
		List<String> friendIds = List.copyOf(friendUserIds);
		combined.addAll(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(
				com.pikume.back.feed.application.dto.FeedVisibility.FRIENDS,
				friendIds,
				sourceQueryLimit));
		combined.addAll(loadDiaryForFeedPort.findFeedIdsByStatusAndUserIds(
				com.pikume.back.feed.application.dto.FeedVisibility.PUBLIC,
				friendIds,
				sourceQueryLimit));
		return List.copyOf(combined);
	}

	private List<Long> loadPublicBucketDiaryIds(String currentUserId, Set<String> friendUserIds, int sourceQueryLimit) {
		List<Long> publicDiaryIds = loadDiaryForFeedPort.findFeedIdsByStatus(
				com.pikume.back.feed.application.dto.FeedVisibility.PUBLIC,
				currentUserId,
				sourceQueryLimit);
		List<Long> anonymousDiaryIds = loadDiaryForFeedPort.findFeedIdsByStatus(
				com.pikume.back.feed.application.dto.FeedVisibility.ANONYMOUS,
				currentUserId,
				sourceQueryLimit);
		LinkedHashSet<Long> publicBucketDiaryIds = new LinkedHashSet<>();
		publicBucketDiaryIds.addAll(publicDiaryIds);
		publicBucketDiaryIds.addAll(anonymousDiaryIds);
		List<Long> diaryIds = List.copyOf(publicBucketDiaryIds);
		if (diaryIds.isEmpty() || friendUserIds.isEmpty()) {
			return diaryIds;
		}

		Map<Long, FeedDiaryCandidateView> diariesById = loadDiaryForFeedPort.getFeedDiaryCandidates(Set.copyOf(publicDiaryIds));
		LinkedHashSet<Long> filtered = publicDiaryIds.stream()
				.filter(diaryId -> {
					FeedDiaryCandidateView diary = diariesById.get(diaryId);
					return diary != null && !friendUserIds.contains(diary.userId());
				})
				.collect(Collectors.toCollection(LinkedHashSet::new));
		filtered.addAll(anonymousDiaryIds);
		return List.copyOf(filtered);
	}

	private Set<Long> resolveConsumedDiaryIds(String currentUserId, Collection<Long> diaryIds) {
		if (!FeedBucket.hasUser(currentUserId) || diaryIds.isEmpty()) {
			return Set.of();
		}

		List<Long> diaryIdList = List.copyOf(diaryIds);
		Set<Long> consumedDiaryIds = new HashSet<>(
				loadFeedClickPort.findClickedDiaryIdsByUserIdAndDiaryIds(currentUserId, diaryIdList));
		consumedDiaryIds.addAll(loadSocialForFeedPort.getLikedDiaryIds(currentUserId, diaryIdList));
		consumedDiaryIds.addAll(loadSocialForFeedPort.getCommentedDiaryIds(currentUserId, diaryIdList));
		return consumedDiaryIds;
	}

	private FeedCursorCandidate toCandidate(FeedBucket bucket, FeedDiaryCandidateView diary, long likeCount, long commentCount) {
		return new FeedCursorCandidate(
				bucket,
				diary.diaryId(),
				likeCount,
				commentCount,
				diary.createdAt());
	}

	private boolean isAfterCursor(FeedCursorCandidate candidate, FeedCursor cursor) {
		if (cursor == null) {
			return true;
		}
		if (candidate.createdAt().isBefore(cursor.createdAt())) {
			return true;
		}
		if (candidate.createdAt().isAfter(cursor.createdAt())) {
			return false;
		}
		if (candidate.likeCount() < cursor.likeCount()) {
			return true;
		}
		if (candidate.likeCount() > cursor.likeCount()) {
			return false;
		}
		if (candidate.commentCount() < cursor.commentCount()) {
			return true;
		}
		if (candidate.commentCount() > cursor.commentCount()) {
			return false;
		}
		return candidate.diaryId() < cursor.diaryId();
	}

	private int initialSourceQueryLimit(int limit) {
		return Math.min(Math.max(limit * SOURCE_QUERY_MULTIPLIER, MIN_SOURCE_QUERY_LIMIT), MAX_SOURCE_QUERY_LIMIT);
	}
}
