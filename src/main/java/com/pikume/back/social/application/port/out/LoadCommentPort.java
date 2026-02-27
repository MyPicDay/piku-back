package com.pikume.back.social.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pikume.back.social.domain.comment.Comment;

import java.util.Optional;

public interface LoadCommentPort {

	Optional<Comment> findById(Long commentId);

	Page<Comment> findVisibleRootCommentsByDiaryId(Long diaryId, Pageable pageable);

	Page<Comment> findByParentIdAndDeletedAtIsNull(Long parentId, Pageable pageable);

	long countAllByDiaryId(Long diaryId);

	int countByParentIdAndDeletedAtIsNull(Long parentId);
}
