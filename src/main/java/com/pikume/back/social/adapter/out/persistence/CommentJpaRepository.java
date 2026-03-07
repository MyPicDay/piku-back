package com.pikume.back.social.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pikume.back.social.domain.comment.Comment;

import java.util.Collection;
import java.util.List;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

	interface ReplyCountProjection {
		Long getParentId();

		long getReplyCount();
	}

	Page<Comment> findByParentIdAndDeletedAtIsNull(Long parentId, Pageable pageable);

	@Query("SELECT COUNT(c) FROM Comment c WHERE c.diaryId = :diaryId")
	long countAllByDiaryId(@Param("diaryId") Long diaryId);

	@Query("SELECT c.parent.id AS parentId, COUNT(c) AS replyCount " +
			"FROM Comment c " +
			"WHERE c.parent.id IN :parentIds AND c.deletedAt IS NULL " +
			"GROUP BY c.parent.id")
	List<ReplyCountProjection> countVisibleRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

	@Query("SELECT c FROM Comment c WHERE c.diaryId = :diaryId AND c.parent IS NULL AND " +
			"(c.deletedAt IS NULL OR EXISTS (SELECT r FROM Comment r WHERE r.parent = c AND r.deletedAt IS NULL))")
	Page<Comment> findVisibleRootCommentsByDiaryId(@Param("diaryId") Long diaryId, Pageable pageable);
}
