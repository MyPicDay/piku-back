package com.pikume.back.social.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.social.adapter.in.web.dto.LikeResponse;
import com.pikume.back.social.application.port.in.LikeUseCase;
import com.pikume.back.social.application.port.out.*;
import com.pikume.back.social.domain.event.SocialEvent;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.social.domain.like.exception.LikeErrorCode;
import com.pikume.back.social.domain.like.exception.LikeException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService implements LikeUseCase {

	private final LoadLikePort loadLikePort;
	private final SaveLikePort saveLikePort;
	private final LoadDiaryInfoPort loadDiaryInfoPort;
	private final PublishEventPort publishEventPort;

	@Override
	@Transactional
	public LikeResponse addLike(String userId, Long diaryId, RequestMetaInfo requestMetaInfo) {
		log.info("[좋아요 추가 요청] userId: {}, diaryId: {}", userId, diaryId);

		String diaryOwnerId = loadDiaryInfoPort.findOwnerUserIdByDiaryId(diaryId)
				.orElseThrow(() -> new LikeException(LikeErrorCode.DIARY_NOT_FOUND));

		if (diaryOwnerId.equals(userId)) {
			throw new LikeException(LikeErrorCode.CANNOT_LIKE_OWN_DIARY);
		}

		Optional<Like> existingLike = loadLikePort.findByUserIdAndDiaryId(userId, diaryId);
		if (existingLike.isPresent()) {
			throw new LikeException(LikeErrorCode.ALREADY_LIKED);
		}

		Like like = Like.builder()
				.userId(userId)
				.diaryId(diaryId)
				.build();
		saveLikePort.save(like);

		publishEventPort.publish(new SocialEvent.LikeCreatedEvent(
				diaryOwnerId, userId, diaryId));

		long likeCount = loadLikePort.countByDiaryId(diaryId);
		log.info("[좋아요 추가 완료] diaryId: {}, 총 좋아요 수: {}", diaryId, likeCount);

		return LikeResponse.builder()
				.diaryId(diaryId)
				.likeCount(likeCount)
				.isLiked(true)
				.build();
	}

	@Override
	@Transactional
	public LikeResponse removeLike(String userId, Long diaryId) {
		log.info("[좋아요 취소 요청] userId: {}, diaryId: {}", userId, diaryId);

		if (!loadDiaryInfoPort.existsById(diaryId)) {
			throw new LikeException(LikeErrorCode.DIARY_NOT_FOUND);
		}

		Like like = loadLikePort.findByUserIdAndDiaryId(userId, diaryId)
				.orElseThrow(() -> new LikeException(LikeErrorCode.LIKE_NOT_FOUND));

		like.inactive();

		long likeCount = loadLikePort.countByDiaryId(diaryId);
		log.info("[좋아요 취소 완료] diaryId: {}, 총 좋아요 수: {}", diaryId, likeCount);

		return LikeResponse.builder()
				.diaryId(diaryId)
				.likeCount(likeCount)
				.isLiked(false)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public LikeResponse getLikeStatus(String userId, Long diaryId) {
		if (!loadDiaryInfoPort.existsById(diaryId)) {
			throw new LikeException(LikeErrorCode.DIARY_NOT_FOUND);
		}

		long likeCount = loadLikePort.countByDiaryId(diaryId);
		boolean isLiked = userId != null && loadLikePort.existsByUserIdAndDiaryId(userId, diaryId);

		return LikeResponse.builder()
				.diaryId(diaryId)
				.likeCount(likeCount)
				.isLiked(isLiked)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public long getLikeCount(Long diaryId) {
		return loadLikePort.countByDiaryId(diaryId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isLikedByUser(String userId, Long diaryId) {
		if (userId == null)
			return false;
		return loadLikePort.existsByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds) {
		if (diaryIds == null || diaryIds.isEmpty()) {
			return Map.of();
		}

		List<Object[]> results = loadLikePort.countByDiaryIds(diaryIds);
		return results.stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> (Long) row[1]));
	}

	@Override
	@Transactional(readOnly = true)
	public Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds) {
		if (userId == null || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return loadLikePort.findLikedDiaryIdsByUserIdAndDiaryIds(userId, diaryIds);
	}
}
