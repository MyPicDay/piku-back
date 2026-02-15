package store.piku.back.social.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.social.domain.comment.Comment;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

	Page<Comment> findByParentIdAndDeletedAtIsNull(Long parentId, Pageable pageable);

	@Query("SELECT COUNT(c) FROM Comment c WHERE c.diaryId = :diaryId")
	long countAllByDiaryId(@Param("diaryId") Long diaryId);

	int countByParentIdAndDeletedAtIsNull(Long parentId);

	@Query("SELECT c FROM Comment c WHERE c.diaryId = :diaryId AND c.parent IS NULL AND " +
			"(c.deletedAt IS NULL OR EXISTS (SELECT r FROM Comment r WHERE r.parent = c AND r.deletedAt IS NULL))")
	Page<Comment> findVisibleRootCommentsByDiaryId(@Param("diaryId") Long diaryId, Pageable pageable);
}
