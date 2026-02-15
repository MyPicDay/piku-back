package store.piku.back.social.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.social.domain.like.Like;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

	@Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.diaryId = :diaryId AND l.deletedAt IS NULL")
	Optional<Like> findByUserIdAndDiaryId(@Param("userId") String userId, @Param("diaryId") Long diaryId);

	@Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Like l WHERE l.userId = :userId AND l.diaryId = :diaryId AND l.deletedAt IS NULL")
	boolean existsByUserIdAndDiaryId(@Param("userId") String userId, @Param("diaryId") Long diaryId);

	@Query("SELECT COUNT(l) FROM Like l WHERE l.diaryId = :diaryId AND l.deletedAt IS NULL")
	long countByDiaryId(@Param("diaryId") Long diaryId);

	@Query("SELECT l.diaryId, COUNT(l) FROM Like l WHERE l.diaryId IN :diaryIds AND l.deletedAt IS NULL GROUP BY l.diaryId")
	List<Object[]> countByDiaryIds(@Param("diaryIds") List<Long> diaryIds);

	@Query("SELECT l.diaryId FROM Like l WHERE l.userId = :userId AND l.diaryId IN :diaryIds AND l.deletedAt IS NULL")
	Set<Long> findLikedDiaryIdsByUserIdAndDiaryIds(@Param("userId") String userId,
			@Param("diaryIds") List<Long> diaryIds);
}
