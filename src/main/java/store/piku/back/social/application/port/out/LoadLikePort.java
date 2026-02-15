package store.piku.back.social.application.port.out;

import store.piku.back.social.domain.like.Like;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoadLikePort {

	Optional<Like> findByUserIdAndDiaryId(String userId, Long diaryId);

	boolean existsByUserIdAndDiaryId(String userId, Long diaryId);

	long countByDiaryId(Long diaryId);

	List<Object[]> countByDiaryIds(List<Long> diaryIds);

	Set<Long> findLikedDiaryIdsByUserIdAndDiaryIds(String userId, List<Long> diaryIds);
}
