package store.piku.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.social.application.port.out.LoadLikePort;
import store.piku.back.social.application.port.out.SaveLikePort;
import store.piku.back.social.domain.like.Like;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LikePersistenceAdapter implements LoadLikePort, SaveLikePort {

	private final LikeJpaRepository likeJpaRepository;

	@Override
	public Optional<Like> findByUserIdAndDiaryId(String userId, Long diaryId) {
		return likeJpaRepository.findByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public boolean existsByUserIdAndDiaryId(String userId, Long diaryId) {
		return likeJpaRepository.existsByUserIdAndDiaryId(userId, diaryId);
	}

	@Override
	public long countByDiaryId(Long diaryId) {
		return likeJpaRepository.countByDiaryId(diaryId);
	}

	@Override
	public List<Object[]> countByDiaryIds(List<Long> diaryIds) {
		return likeJpaRepository.countByDiaryIds(diaryIds);
	}

	@Override
	public Set<Long> findLikedDiaryIdsByUserIdAndDiaryIds(String userId, List<Long> diaryIds) {
		return likeJpaRepository.findLikedDiaryIdsByUserIdAndDiaryIds(userId, diaryIds);
	}

	@Override
	public Like save(Like like) {
		return likeJpaRepository.save(like);
	}
}
