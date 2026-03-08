package com.pikume.back.feed.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.application.port.out.PhotoStoragePort;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.feed.application.port.out.LoadFeedListViewPort;
import com.pikume.back.feed.application.readmodel.FeedListItemView;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.domain.friend.vo.FriendStatus;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedListViewPersistenceAdapter implements LoadFeedListViewPort {

	private final DiaryJpaRepository diaryJpaRepository;
	private final PhotoJpaRepository photoJpaRepository;
	private final UserJpaRepository userJpaRepository;
	private final FriendJpaRepository friendJpaRepository;
	private final FriendRequestJpaRepository friendRequestJpaRepository;
	private final CommentJpaRepository commentJpaRepository;
	private final LikeJpaRepository likeJpaRepository;
	private final PhotoStoragePort photoStoragePort;

	@Override
	public List<FeedListItemView> loadFeedListItems(List<Long> diaryIds, String currentUserId) {
		if (diaryIds.isEmpty()) {
			return List.of();
		}

		Map<Long, Diary> diariesById = diaryJpaRepository.findByIdInAndDeletedAtIsNull(diaryIds).stream()
				.collect(Collectors.toMap(Diary::getId, Function.identity()));
		Map<Long, List<String>> photosByDiaryId = loadPhotoUrls(diaryIds);
		Map<String, User> usersById = loadUsers(diariesById.values());
		Map<String, FriendStatus> friendStatusesByUserId = loadFriendStatuses(currentUserId, usersById.keySet());
		Map<Long, Long> commentCountsByDiaryId = loadCommentCounts(diaryIds);
		Map<Long, Long> likeCountsByDiaryId = loadLikeCounts(diaryIds);
		Set<Long> likedDiaryIds = loadLikedDiaryIds(currentUserId, diaryIds);

		return diaryIds.stream()
				.map(diariesById::get)
				.filter(java.util.Objects::nonNull)
				.map(diary -> toFeedListItemView(
						diary,
						photosByDiaryId,
						usersById,
						friendStatusesByUserId,
						commentCountsByDiaryId,
						likeCountsByDiaryId,
						likedDiaryIds))
				.toList();
	}

	private Map<Long, List<String>> loadPhotoUrls(List<Long> diaryIds) {
		Map<Long, List<String>> photoUrlsByDiaryId = new HashMap<>();

		for (Photo photo : photoJpaRepository.findByDiaryIds(diaryIds)) {
			photoUrlsByDiaryId.computeIfAbsent(photo.getDiary().getId(), ignored -> new java.util.ArrayList<>())
					.add(photoStoragePort.getPhotoUrl(photo.getUrl(), Boolean.TRUE.equals(photo.getRepresent())));
		}

		return photoUrlsByDiaryId;
	}

	private Map<String, User> loadUsers(java.util.Collection<Diary> diaries) {
		Set<String> userIds = diaries.stream()
				.map(Diary::getUserId)
				.collect(Collectors.toSet());
		if (userIds.isEmpty()) {
			return Map.of();
		}

		return userJpaRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
	}

	private Map<String, FriendStatus> loadFriendStatuses(String currentUserId, Set<String> targetUserIds) {
		if (currentUserId == null || currentUserId.isBlank() || targetUserIds.isEmpty()) {
			return Map.of();
		}

		Set<String> filteredTargetIds = new HashSet<>(targetUserIds);
		filteredTargetIds.remove(currentUserId);
		if (filteredTargetIds.isEmpty()) {
			return Map.of();
		}

		Map<String, FriendStatus> statuses = new HashMap<>();
		friendJpaRepository.findFriendIdsWithinTargets(currentUserId, filteredTargetIds)
				.forEach(friendId -> statuses.put(friendId, FriendStatus.FRIENDS));

		Set<String> unresolvedTargetIds = filteredTargetIds.stream()
				.filter(targetUserId -> !statuses.containsKey(targetUserId))
				.collect(Collectors.toSet());
		if (unresolvedTargetIds.isEmpty()) {
			return statuses;
		}

		friendRequestJpaRepository.findRequestedTargetIds(currentUserId, unresolvedTargetIds)
				.forEach(targetUserId -> statuses.put(targetUserId, FriendStatus.REQUESTED));

		Set<String> requestedResolvedTargetIds = unresolvedTargetIds.stream()
				.filter(targetUserId -> !statuses.containsKey(targetUserId))
				.collect(Collectors.toSet());
		if (requestedResolvedTargetIds.isEmpty()) {
			return statuses;
		}

		friendRequestJpaRepository.findReceivedFromUserIds(currentUserId, requestedResolvedTargetIds)
				.forEach(targetUserId -> statuses.put(targetUserId, FriendStatus.RECEIVED));

		return statuses;
	}

	private Map<Long, Long> loadCommentCounts(List<Long> diaryIds) {
		return commentJpaRepository.countAllByDiaryIds(diaryIds).stream()
				.collect(Collectors.toMap(
						CommentJpaRepository.DiaryCommentCountProjection::getDiaryId,
						CommentJpaRepository.DiaryCommentCountProjection::getCommentCount));
	}

	private Map<Long, Long> loadLikeCounts(List<Long> diaryIds) {
		return likeJpaRepository.countByDiaryIds(diaryIds).stream()
				.collect(Collectors.toMap(
						result -> (Long) result[0],
						result -> (Long) result[1]));
	}

	private Set<Long> loadLikedDiaryIds(String currentUserId, List<Long> diaryIds) {
		if (currentUserId == null || currentUserId.isBlank()) {
			return Set.of();
		}
		return likeJpaRepository.findLikedDiaryIdsByUserIdAndDiaryIds(currentUserId, diaryIds);
	}

	private FeedListItemView toFeedListItemView(Diary diary,
			Map<Long, List<String>> photosByDiaryId,
			Map<String, User> usersById,
			Map<String, FriendStatus> friendStatusesByUserId,
			Map<Long, Long> commentCountsByDiaryId,
			Map<Long, Long> likeCountsByDiaryId,
			Set<Long> likedDiaryIds) {
		User user = usersById.get(diary.getUserId());

		return new FeedListItemView(
				diary.getId(),
				diary.getStatus(),
				diary.getContent(),
				photosByDiaryId.getOrDefault(diary.getId(), List.of()),
				diary.getDate(),
				user != null ? user.getNickname() : "알 수 없음",
				user != null ? user.getAvatar() : null,
				diary.getUserId(),
				diary.getCreatedAt(),
				friendStatusesByUserId.getOrDefault(diary.getUserId(), FriendStatus.NONE),
				commentCountsByDiaryId.getOrDefault(diary.getId(), 0L),
				likeCountsByDiaryId.getOrDefault(diary.getId(), 0L),
				likedDiaryIds.contains(diary.getId()));
	}
}
