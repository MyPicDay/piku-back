package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.out.LoadDiaryLikesPort;
import com.pikume.back.social.application.port.out.RecordDiaryLikePort;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import com.pikume.back.social.domain.like.Like;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LikePersistenceAdapter implements LoadDiaryLikesPort, RecordDiaryLikePort {

	private final LikeJpaRepository likeJpaRepository;

	@Override
	public Optional<Like> loadActiveLike(String userId, Long diaryId) {
		return likeJpaRepository.findByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public Optional<Like> loadLikeState(String userId, Long diaryId) {
		return likeJpaRepository.findAnyByUserIdAndDiaryIdForUpdate(userId, diaryId);
	}

	@Override
	public boolean activeLikeExists(String userId, Long diaryId) {
		return likeJpaRepository.existsByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public long countActiveLikes(Long diaryId) {
		return likeJpaRepository.countByDiaryId(diaryId);
	}

	@Override
	public List<DiaryEngagementCount> loadActiveLikeCounts(List<Long> diaryIds) {
		return likeJpaRepository.countByDiaryIds(diaryIds).stream()
				.map(row -> new DiaryEngagementCount((Long) row[0], ((Number) row[1]).longValue()))
				.toList();
	}

	@Override
	public Set<Long> loadLikedDiaryIds(String userId, List<Long> diaryIds) {
		return likeJpaRepository.findLikedDiaryIdsByUserIdAndDiaryIds(userId, diaryIds);
	}

	@Override
	public Like recordLike(Like like) {
		try {
			return likeJpaRepository.saveAndFlush(like);
		} catch (DataIntegrityViolationException exception) {
			if (like.getId() == null) {
				throw new SocialException(SocialErrorCode.DUPLICATE_LIKE, exception);
			}
			throw exception;
		}
	}
}
