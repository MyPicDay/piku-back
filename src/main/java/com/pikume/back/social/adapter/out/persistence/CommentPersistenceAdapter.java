package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadCommentPort;
import com.pikume.back.social.application.port.out.SaveCommentPort;
import com.pikume.back.social.domain.comment.Comment;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentPort, SaveCommentPort {

	private final CommentJpaRepository commentJpaRepository;

	@Override
	public Optional<Comment> findById(Long commentId) {
		return commentJpaRepository.findById(commentId);
	}

	@Override
	public Page<Comment> findVisibleRootCommentsByDiaryId(Long diaryId, Pageable pageable) {
		return commentJpaRepository.findVisibleRootCommentsByDiaryId(diaryId, pageable);
	}

	@Override
	public Page<Comment> findByParentIdAndDeletedAtIsNull(Long parentId, Pageable pageable) {
		return commentJpaRepository.findByParentIdAndDeletedAtIsNull(parentId, pageable);
	}

	@Override
	public long countAllByDiaryId(Long diaryId) {
		return commentJpaRepository.countAllByDiaryId(diaryId);
	}

	@Override
	public int countByParentIdAndDeletedAtIsNull(Long parentId) {
		return commentJpaRepository.countByParentIdAndDeletedAtIsNull(parentId);
	}

	@Override
	public Comment save(Comment comment) {
		return commentJpaRepository.save(comment);
	}
}
