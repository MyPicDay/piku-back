package com.pikume.back.social.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.social.application.port.out.LoadCommentPort;
import com.pikume.back.social.application.port.out.SaveCommentPort;
import com.pikume.back.social.domain.comment.Comment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentPort, SaveCommentPort {

	private final CommentJpaRepository commentJpaRepository;

	@Override
	public Optional<Comment> findById(Long commentId) {
		return commentJpaRepository.findById(commentId);
	}

	@Override
	public long countActiveCommentsByDiaryId(Long diaryId) {
		return commentJpaRepository.countActiveCommentsByDiaryId(diaryId);
	}

	@Override
	public List<Object[]> countActiveCommentsByDiaryIds(Collection<Long> diaryIds) {
		return commentJpaRepository.countActiveCommentsByDiaryIds(diaryIds).stream()
				.map(result -> new Object[] { result.getDiaryId(), result.getCommentCount() })
				.toList();
	}

	@Override
	public Set<Long> findCommentedDiaryIdsByUserId(String userId, Collection<Long> diaryIds) {
		if (userId == null || diaryIds == null || diaryIds.isEmpty()) {
			return Set.of();
		}
		return commentJpaRepository.findCommentedDiaryIdsByUserIdAndDiaryIdIn(userId, diaryIds);
	}

	@Override
	public Comment save(Comment comment) {
		return commentJpaRepository.save(comment);
	}
}
