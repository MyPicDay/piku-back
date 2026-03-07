package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
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
	public long countAllByDiaryId(Long diaryId) {
		return commentJpaRepository.countAllByDiaryId(diaryId);
	}

	@Override
	public Comment save(Comment comment) {
		return commentJpaRepository.save(comment);
	}
}
